package solution.offline;

import org.pi4.locutil.GeoPosition;
import org.pi4.locutil.MACAddress;
import org.pi4.locutil.trace.TraceEntry;
import solution.utilities.Helpers;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.log;

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
        this.unhearableThreshold = Double.NEGATIVE_INFINITY;
    }
    
    public ModelBasedStrategy(Map<MACAddress, GeoPosition> accessPointPositions, double n, double d0, double p_d0, double unhearableThreshold) {
        this.accessPointPositions = accessPointPositions;

        this.n = n;
        this.d0 = d0;
        this.p_d0 = p_d0;
        this.unhearableThreshold = unhearableThreshold;
    }

    public RadioMap createRadioMap(Set<TraceEntry> entries) {
        RadioMap radioMap = new RadioMap();

        Map<GeoPosition, Set<TraceEntry>> entriesByPosition = Helpers.groupEntriesByPosition(entries);
        Set<GeoPosition> allPositions = entriesByPosition.keySet();

        for (GeoPosition position : allPositions) {
            Map<MACAddress, Double> distances = accessPointPositions.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> position.distance(entry.getValue())));
            
            Map<MACAddress, Double> signalStrengths = distances.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> signalStrength(entry.getValue())));
            
            Map<MACAddress, Double> filteredSignalStrengths = signalStrengths.entrySet().stream()
                    .filter(entry -> Double.compare(entry.getValue(), unhearableThreshold) > 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            
            radioMap.put(position, filteredSignalStrengths);
        }
        
        return radioMap;
    }

    private double signalStrength(double d) {
        return p_d0 - 10.0 * n * log(d / d0);
    }
}
