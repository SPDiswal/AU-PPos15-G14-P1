package solution.offline;

import org.pi4.locutil.*;
import org.pi4.locutil.trace.TraceEntry;

import java.util.*;
import java.util.stream.Collectors;

public class EmpiricalStrategy implements RadioMapStrategy
{
    public RadioMap createRadioMap(List<TraceEntry> entries)
    {
        RadioMap radioMap = new RadioMap();
        
        Set<GeoPosition> positions = entries.stream().map(TraceEntry::getGeoPosition).collect(Collectors.toSet());
        
        for (GeoPosition position : positions)
        {
            Set<TraceEntry> entriesAtPosition = entries.stream().filter(e -> e.getGeoPosition().equals(position))
                                                       .collect(Collectors.toSet());
            
            Set<MACAddress> accessPoints = entriesAtPosition.stream()
                                                            .flatMap(e -> e.getSignalStrengthSamples().keySet()
                                                                           .stream())
                                                            .collect(Collectors.toSet());
    
            radioMap.put(position, new HashMap<>());
            
            for (MACAddress accessPoint : accessPoints)
            {
                double average = entriesAtPosition.stream()
                                                  .map(TraceEntry::getSignalStrengthSamples)
                                                  .filter(s -> s.containsKey(accessPoint))
                                                  .flatMap(s -> s.getSignalStrengthValues(accessPoint).stream())
                                                  .mapToDouble(s -> s)
                                                  .average().getAsDouble();
                
                radioMap.get(position).put(accessPoint, average);
            }
        }
        
        return radioMap;
    }
}
