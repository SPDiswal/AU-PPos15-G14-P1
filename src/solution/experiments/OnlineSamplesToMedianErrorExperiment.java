package solution.experiments;

import org.pi4.locutil.io.TraceGenerator;
import solution.*;
import solution.offline.*;
import solution.online.*;
import solution.utilities.Helpers;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toList;

/**
 * Fourth experiment of Part II.
 */
public class OnlineSamplesToMedianErrorExperiment implements ExperimentStrategy
{
    private static final int LOWER_SAMPLE_SIZE = 1;
    private static final int UPPER_SAMPLE_SIZE = 10;
    
    private final FingerprintingStrategy fingerprintingStrategy;
    private final EstimationStrategy estimationStrategy;
    
    public OnlineSamplesToMedianErrorExperiment(FingerprintingStrategy fingerprintingStrategy,
                                                EstimationStrategy estimationStrategy)
    {
        this.fingerprintingStrategy = fingerprintingStrategy;
        this.estimationStrategy = estimationStrategy;
    }
    
    @Override
    public List<DoublePair> runExperiment() throws IOException
    {
        List<DoublePair> results = new ArrayList<>();
    
        for (int onlineSampleSize = LOWER_SAMPLE_SIZE; onlineSampleSize <= UPPER_SAMPLE_SIZE; onlineSampleSize++)
        {
            double medianError = run(onlineSampleSize);
            results.add(DoublePair.from(onlineSampleSize, medianError));
        }
        
        return results;
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
    
    private double run(int onlineSampleSize) throws IOException
    {
        TraceGenerator traceGenerator = Helpers.loadTraces(Constants.OFFLINE_TRACES, Constants.ONLINE_TRACES,
                                                           Constants.OFFLINE_SAMPLE_SIZE, onlineSampleSize);
        
        RadioMap radioMap = Helpers.train(traceGenerator, fingerprintingStrategy);
        Set<GeoPositionPair> results = Helpers.test(traceGenerator, estimationStrategy, radioMap);
        List<Double> errors = Helpers.computeErrors(results);
        
        return Helpers.median(errors);
    }
}
