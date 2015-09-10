package solution.online;

import org.pi4.locutil.GeoPosition;
import org.pi4.locutil.MACAddress;
import solution.offline.RadioMap;
import solution.utilities.Helpers;

import java.util.AbstractMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class KNearestNeighbourStrategy implements EstimationStrategy {
    private int k;

    public KNearestNeighbourStrategy(int k) {
        this.k = k;
    }

    public GeoPosition estimatePosition(RadioMap radioMap, Map<MACAddress, Double> measurements) {
        if (radioMap.keySet().isEmpty()) return GeoPosition.parse("NaN NaN NaN");
        
        PriorityQueue<Map.Entry<GeoPosition, Double>> queue = new PriorityQueue<>((a, b) -> -Double.compare(a.getValue(), b.getValue()));

        Set<MACAddress> visibleAccessPoints = measurements.keySet();

        for (GeoPosition position : radioMap.keySet()) {
            Set<MACAddress> availableAccessPoints = radioMap.get(position).keySet();
            Set<MACAddress> accessPointsInCommon = Helpers.intersection(visibleAccessPoints, availableAccessPoints);

            if (accessPointsInCommon.isEmpty()) return GeoPosition.parse("NaN NaN NaN");

            double sumOfSignalsSquared
                    = accessPointsInCommon.stream()
                    .map(accessPoint -> pow(measurements.get(accessPoint)
                            - radioMap.get(position).get(accessPoint), 2))
                    .reduce(0.0, (sum, signalStrengthDifferenceSquared) ->
                            sum + signalStrengthDifferenceSquared);
            
            double squareRoot = sqrt(sumOfSignalsSquared);
            
            queue.add(new AbstractMap.SimpleEntry<>(position, squareRoot));

            if (queue.size() > k) {
                queue.poll();
            }
        }
        
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
