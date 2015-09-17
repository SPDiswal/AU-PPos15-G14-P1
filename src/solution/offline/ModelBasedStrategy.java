package solution.offline;

import org.pi4.locutil.*;
import org.pi4.locutil.trace.TraceEntry;
import solution.Constants;
import solution.utilities.Helpers;

import java.util.*;

import static java.util.stream.Collectors.toMap;

public class ModelBasedStrategy implements FingerprintingStrategy
{
    private Map<MACAddress, GeoPosition> accessPointPositions;
    
    private final double n;
    private final double d0;
    private final double p_d0;
    private final double unhearableThreshold;
    
    public ModelBasedStrategy(Map<MACAddress, GeoPosition> accessPointPositions)
    {
        this.accessPointPositions = accessPointPositions;
        
        this.n = 3.415;
        this.d0 = 1;
        this.p_d0 = -33.77;
        this.unhearableThreshold = Constants.UNHEARABLE_THRESHOLD;
    }
    
    public ModelBasedStrategy(Map<MACAddress, GeoPosition> accessPointPositions,
                              double n,
                              double d0,
                              double p_d0,
                              double unhearableThreshold)
    {
        this.accessPointPositions = accessPointPositions;
        
        this.n = n;
        this.d0 = d0;
        this.p_d0 = p_d0;
        this.unhearableThreshold = unhearableThreshold;
    }
    
    public RadioMap createRadioMap(Set<TraceEntry> entries)
    {
        RadioMap radioMap = new RadioMap();
        
        Map<GeoPosition, Set<TraceEntry>> entriesByPosition = Helpers.groupEntriesByPosition(entries);
        Set<GeoPosition> allPositions = entriesByPosition.keySet();
        
        for (GeoPosition position : allPositions)
        {
            Map<MACAddress, Double> distances = accessPointPositions.entrySet()
                                                                    .stream()
                                                                    .collect(toMap(Map.Entry::getKey,
                                                                                   entry -> position
                                                                                           .distance(entry.getValue())));
            
            Map<MACAddress, Double> signals = distances.entrySet()
                                                       .stream()
                                                       .collect(toMap(Map.Entry::getKey,
                                                                      entry -> signalStrength(entry.getValue())));
            
            Map<MACAddress, Double> hearableSignals = signals.entrySet()
                                                             .stream()
                                                             .filter(entry -> Double.compare(entry.getValue(),
                                                                                             unhearableThreshold) > 0)
                                                             .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            
            radioMap.put(position, hearableSignals);
        }
        
        return radioMap;
    }
    
    private double signalStrength(double d)
    {
        return p_d0 - 10.0 * n * Math.log10(d / d0);
    }
}
