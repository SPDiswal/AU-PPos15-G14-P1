package solution.experiments;

import org.pi4.locutil.*;
import org.pi4.locutil.io.TraceGenerator;
import solution.Constants;
import solution.offline.*;
import solution.utilities.Helpers;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * First experiment of Part II.
 */
public class DistanceToSignalStrengthExperiment implements ExperimentStrategy
{
    private final FingerprintingStrategy fingerprintingStrategy;
    
    public DistanceToSignalStrengthExperiment(FingerprintingStrategy fingerprintingStrategy)
    {
        this.fingerprintingStrategy = fingerprintingStrategy;
    }
    
    @Override
    public List<DoublePair> runExperiment() throws IOException
    {
        TraceGenerator traceGenerator = Helpers.loadTraces(Constants.OFFLINE_TRACES, Constants.ONLINE_TRACES,
                                                           Constants.OFFLINE_SAMPLE_SIZE, Constants.ONLINE_SAMPLE_SIZE);
        Map<MACAddress, GeoPosition> accessPointPositions = Helpers.loadAccessPoints(Constants.ACCESS_POINT_POSITIONS);
        
        RadioMap radioMap = Helpers.train(traceGenerator, fingerprintingStrategy);
        List<DoublePair> resultList = new ArrayList<>();
        
        for (GeoPosition position : radioMap.keySet())
        {
            for (MACAddress ap : radioMap.get(position).keySet())
            {
                GeoPosition apPosition = accessPointPositions.get(ap);
                if (apPosition == null)
                {
                    continue;
                }
                Double distance = position.distance(apPosition);
                Double signalStrength = radioMap.get(position).get(ap);
                resultList.add(new DoublePair(distance, signalStrength));
            }
        }
        
        return resultList;
    }
    
    @Override
    public List<DoublePair> aggregateResults(Collection<DoublePair> results)
    {
        Map<Double, List<Double>> groups = results.stream()
                                                  .collect(groupingBy(DoublePair::getFirst,
                                                                      mapping(DoublePair::getSecond, toList())));
        
        return groups.entrySet()
                     .stream()
                     .map(e -> DoublePair.from(e.getKey(),
                                               e.getValue().stream().mapToDouble(d -> d).average().getAsDouble()))
                     .sorted((a, b) -> Double.compare(a.getFirst(), b.getFirst()))
                     .collect(toList());
    }
}
