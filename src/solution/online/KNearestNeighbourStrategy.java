package solution.online;

import org.pi4.locutil.*;
import solution.Constants;
import solution.offline.RadioMap;
import solution.utilities.Helpers;

import java.util.*;

import static java.lang.Math.*;

public class KNearestNeighbourStrategy implements EstimationStrategy
{
    private static final int COMMON_ACCESS_POINTS_THRESHOLD = 3;
    
    private int k;
    
    public KNearestNeighbourStrategy(int k)
    {
        this.k = k;
    }
    
    public GeoPosition estimatePosition(RadioMap radioMap, Map<MACAddress, Double> measurements)
    {
        if (radioMap.keySet().isEmpty()) return GeoPosition.parse("NaN NaN NaN");
        
        PriorityQueue<Map.Entry<GeoPosition, Double>> queue =
                new PriorityQueue<>((a, b) -> -Double.compare(a.getValue(), b.getValue()));
        
        Set<MACAddress> measuredAccessPoints = measurements.keySet();
        
        for (GeoPosition position : radioMap.keySet())
        {
            if (radioMap.get(position).isEmpty()) continue;
            
            Set<MACAddress> knownAccessPoints = radioMap.get(position).keySet();
            Set<MACAddress> accessPointsInCommon = Helpers.intersection(measuredAccessPoints, knownAccessPoints);
            Set<MACAddress> allAccessPoints = Helpers.union(measuredAccessPoints, knownAccessPoints);
            
            double sum = (accessPointsInCommon.size() >= COMMON_ACCESS_POINTS_THRESHOLD ? accessPointsInCommon
                                                                                        : allAccessPoints)
                    .stream()
                    .mapToDouble(a -> pow(measurements.getOrDefault(a, Constants.UNHEARABLE_THRESHOLD)
                                          - radioMap.get(position).getOrDefault(a, Constants.UNHEARABLE_THRESHOLD), 2))
                    .sum();
            
            double signalDistance = sqrt(sum);
            
            queue.add(new AbstractMap.SimpleEntry<>(position, signalDistance));
            if (queue.size() > k) queue.poll();
        }
        
        if (queue.isEmpty()) return GeoPosition.parse("NaN NaN NaN");
        
        int numberOfPositions = queue.size();
        
        GeoPosition positionSum = queue.stream()
                                       .map(Map.Entry::getKey)
                                       .reduce(GeoPosition.parse("0 0 0"), GeoPosition::addPosition);
        
        double x = positionSum.getX() / numberOfPositions;
        double y = positionSum.getY() / numberOfPositions;
        double z = positionSum.getZ() / numberOfPositions;
        
        return new GeoPosition(x, y, z);
    }
}
