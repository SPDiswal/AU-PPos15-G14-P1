package solution.offline;

import org.pi4.locutil.*;
import org.pi4.locutil.trace.TraceEntry;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class EmpiricalStrategy implements RadioMapStrategy
{
    public RadioMap createRadioMap(Set<TraceEntry> entries)
    {
        RadioMap radioMap = new RadioMap();
        
        Map<GeoPosition, Set<TraceEntry>> entriesByPosition = groupEntriesByPosition(entries);
        Set<GeoPosition> allPositions = entriesByPosition.keySet();
        
        for (GeoPosition position : allPositions)
        {
            Set<TraceEntry> entriesAtPosition = entriesByPosition.get(position);
            Set<MACAddress> accessPoints = getAvailableAccessPoints(entriesAtPosition);
            
            Map<MACAddress, Double> averageSamples = computeAverageSignalStrengthByAccessPoint(entriesAtPosition,
                                                                                               accessPoints);
            
            radioMap.put(position, averageSamples);
        }
        
        return radioMap;
    }
    
    private static Map<GeoPosition, Set<TraceEntry>> groupEntriesByPosition(Set<TraceEntry> entries)
    {
        Map<GeoPosition, Set<TraceEntry>> result = new HashMap<>();
        
        Set<GeoPosition> allPositions = entries.stream()
                                               .map(TraceEntry::getGeoPosition)
                                               .collect(toSet());
        
        for (GeoPosition position : allPositions)
        {
            Set<TraceEntry> entriesAtPosition = entries.stream()
                                                       .filter(entry -> entry.getGeoPosition().equals(position))
                                                       .collect(toSet());
            
            result.put(position, entriesAtPosition);
        }
        
        return Collections.unmodifiableMap(result);
    }
    
    private static Set<MACAddress> getAvailableAccessPoints(Set<TraceEntry> entriesAtPosition)
    {
        Set<MACAddress> accessPoints = entriesAtPosition.stream()
                                                        .flatMap(entry -> entry.getSignalStrengthSamples().keySet()
                                                                               .stream())
                                                        .collect(toSet());
        
        return Collections.unmodifiableSet(accessPoints);
    }
    
    private static Map<MACAddress, Double> computeAverageSignalStrengthByAccessPoint(Set<TraceEntry> entriesAtPosition,
                                                                                     Set<MACAddress> accessPoints)
    {
        Map<MACAddress, Double> result = new HashMap<>();
        
        for (MACAddress accessPoint : accessPoints)
        {
            double averageSignalStrength = entriesAtPosition.stream()
                                                            .map(TraceEntry::getSignalStrengthSamples)
                                                            .filter(samples -> samples.containsKey(accessPoint))
                                                            .mapToDouble(samples -> samples
                                                                    .getAverageSignalStrength(accessPoint))
                                                            .average().getAsDouble();
            
            result.put(accessPoint, averageSignalStrength);
        }
        
        return Collections.unmodifiableMap(result);
    }
}
