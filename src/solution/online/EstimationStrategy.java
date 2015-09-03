package solution.online;

import org.pi4.locutil.*;
import solution.offline.RadioMap;

import java.util.Map;

public interface EstimationStrategy
{
    GeoPosition estimatePosition(RadioMap radioMap, Map<MACAddress, Double> measurements);
}
