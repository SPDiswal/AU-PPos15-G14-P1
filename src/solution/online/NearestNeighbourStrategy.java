package solution.online;

import org.pi4.locutil.*;
import solution.offline.RadioMap;

import java.util.*;

public class NearestNeighbourStrategy implements EstimationStrategy
{
    public GeoPosition estimatePosition(RadioMap radioMap, Map<MACAddress, Double> measurements)
    {
        Set<MACAddress> visibleAccessPoints = measurements.keySet();
        
        for (GeoPosition position : radioMap.keySet())
        {
            Set<MACAddress> availableAccessPoints = radioMap.get(position).keySet();
            availableAccessPoints.retainAll(visibleAccessPoints);
            
            
        }
        
        throw new UnsupportedOperationException();
    }
}
