package solution.utilities;

import org.pi4.locutil.*;
import org.pi4.locutil.trace.TraceEntry;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class Helpers
{
    public static Map<GeoPosition, Set<TraceEntry>> groupEntriesByPosition(Set<TraceEntry> entries)
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
    
    public static Set<MACAddress> getAvailableAccessPoints(Set<TraceEntry> entriesAtPosition)
    {
        Set<MACAddress> accessPoints = entriesAtPosition.stream()
                                                        .flatMap(entry -> entry.getSignalStrengthSamples().keySet()
                                                                               .stream())
                                                        .collect(toSet());
        
        return Collections.unmodifiableSet(accessPoints);
    }
    
    public static Map<MACAddress, Double> computeAverageSignalStrengthByAccessPoint(Set<TraceEntry> entriesAtPosition,
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
    
    public static <T> Set<T> intersection(Set<T> a, Set<T> b)
    {
        return a.stream().filter(b::contains).collect(toSet());
    }
    
    public static double median(Collection<Double> values)
    {
        List<Double> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);
        
        if (sortedValues.size() % 2 == 1)
        {
            return sortedValues.get((sortedValues.size() + 1) / 2 - 1);
        }
        else
        {
            double lower = sortedValues.get(sortedValues.size() / 2 - 1);
            double upper = sortedValues.get(sortedValues.size() / 2);
            
            return (lower + upper) / 2.0;
        }
    }
}
