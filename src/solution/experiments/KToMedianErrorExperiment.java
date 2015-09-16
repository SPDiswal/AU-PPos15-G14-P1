package solution.experiments;

import org.pi4.locutil.io.TraceGenerator;
import solution.*;
import solution.offline.*;
import solution.online.KNearestNeighbourStrategy;
import solution.utilities.Helpers;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Third experiment of Part II.
 */
public class KToMedianErrorExperiment implements ExperimentStrategy
{
    private final EmpiricalStrategy fingerprintingStrategy;
    
    public KToMedianErrorExperiment(EmpiricalStrategy fingerprintingStrategy)
    {
        this.fingerprintingStrategy = fingerprintingStrategy;
    }
    
    public List<DoublePair> runExperiment() throws IOException
    {
        List<DoublePair> results = new ArrayList<>();
        
        TraceGenerator traceGenerator = Helpers.loadTraces(Constants.OFFLINE_TRACES, Constants.ONLINE_TRACES,
                                                           Constants.OFFLINE_SIZE, Constants.ONLINE_SIZE);
        
        for (int k = 1; k <= 5; k++)
        {
            double medianError = run(k, traceGenerator, fingerprintingStrategy);
            results.add(DoublePair.from(k, medianError));
        }
        
        return results;
    }
    
    private double run(int k,
                       TraceGenerator traceGenerator,
                       FingerprintingStrategy fingerprintingStrategy) throws IOException
    {
        RadioMap radioMap = Helpers.train(traceGenerator, fingerprintingStrategy);
        Set<GeoPositionPair> results = Helpers.test(traceGenerator, new KNearestNeighbourStrategy(k), radioMap);
        List<Double> errors = Helpers.computeErrors(results);
        
        return Helpers.median(errors);
    }
    
    public List<DoublePair> aggregateResults(List<DoublePair> results)
    {
        Map<Double, List<Double>> groups = results.stream()
                                                  .collect(groupingBy(DoublePair::getFirst,
                                                                      mapping(DoublePair::getSecond, toList())));
        
        return groups.entrySet()
                     .stream()
                     .map(e -> DoublePair.from(e.getKey(),
                                               e.getValue().stream().mapToDouble(i -> i).average().getAsDouble()))
                     .collect(toList());
    }
}
