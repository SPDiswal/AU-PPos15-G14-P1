package solution.offline;

import org.pi4.locutil.*;
import org.pi4.locutil.trace.TraceEntry;
import solution.utilities.Helpers;

import java.util.*;

public class EmpiricalStrategy implements RadioMapStrategy
{
    public RadioMap createRadioMap(Set<TraceEntry> entries)
    {
        RadioMap radioMap = new RadioMap();
        
        Map<GeoPosition, Set<TraceEntry>> entriesByPosition = Helpers.groupEntriesByPosition(entries);
        Set<GeoPosition> allPositions = entriesByPosition.keySet();
        
        for (GeoPosition position : allPositions)
        {
            Set<TraceEntry> entriesAtPosition = entriesByPosition.get(position);
            Set<MACAddress> accessPoints = Helpers.getAvailableAccessPoints(entriesAtPosition);
            
            Map<MACAddress, Double> averageSamples = Helpers
                    .computeAverageSignalStrengthByAccessPoint(entriesAtPosition,
                                                               accessPoints);
            
            radioMap.put(position, averageSamples);
        }
        
        return radioMap;
    }
}
