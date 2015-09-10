package solution.online;

import org.pi4.locutil.GeoPosition;
import org.pi4.locutil.MACAddress;
import solution.offline.RadioMap;

import java.util.Map;

public class NearestNeighbourStrategy implements EstimationStrategy {
    private KNearestNeighbourStrategy kNearestNeighbourStrategy = new KNearestNeighbourStrategy(1);

    @Override
    public GeoPosition estimatePosition(RadioMap radioMap, Map<MACAddress, Double> measurements) {
        return kNearestNeighbourStrategy.estimatePosition(radioMap, measurements);
    }
}
