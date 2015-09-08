package solution.online;

import org.pi4.locutil.*;
import solution.offline.RadioMap;
import solution.utilities.Helpers;

import java.util.*;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.stream.Collectors.toSet;

public class NearestNeighbourStrategy implements EstimationStrategy
{
    public GeoPosition estimatePosition(RadioMap radioMap, Map<MACAddress, Double> measurements)
    {
        GeoPosition bestPositionSoFar = GeoPosition.parse("NaN NaN NaN");
        double bestEstimateSoFar = Double.MAX_VALUE;
        
        Set<MACAddress> visibleAccessPoints = measurements.keySet();
        
        for (GeoPosition position : radioMap.keySet())
        {
            Set<MACAddress> availableAccessPoints = radioMap.get(position).keySet();
            Set<MACAddress> accessPointsInCommon = Helpers.intersection(visibleAccessPoints, availableAccessPoints);
            
            if (accessPointsInCommon.isEmpty()) return GeoPosition.parse("NaN NaN NaN");
            
            double sumOfSignalsSquared
                    = accessPointsInCommon.stream()
                                          .map(accessPoint -> pow(
                                                  measurements.get(accessPoint)
                                                  - radioMap.get(position).get(accessPoint), 2))
                                          .reduce(0.0, (sum, signalStrengthDifferenceSquared) ->
                                                  sum + signalStrengthDifferenceSquared);
            
            double squareRoot = sqrt(sumOfSignalsSquared);
            
            if (Double.compare(squareRoot, bestEstimateSoFar) < 0)
            {
                bestPositionSoFar = position;
                bestEstimateSoFar = squareRoot;
            }
        }
        
        return bestPositionSoFar;
    }
}
