package solution.experiments;

import org.pi4.locutil.io.TraceGenerator;
import solution.Constants;
import solution.GeoPositionPair;
import solution.offline.FingerprintingStrategy;
import solution.offline.RadioMap;
import solution.online.EstimationStrategy;
import solution.utilities.Helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * Created by Thomas on 16-09-2015.
 */
public class ErrorFunctionExperiment implements ExperimentStrategy{

    private final FingerprintingStrategy fingerprintingStrategy;
    private final EstimationStrategy estimationStrategy;

    public ErrorFunctionExperiment(FingerprintingStrategy fingerprintingStrategy, EstimationStrategy estimationStrategy)
    {

        this.fingerprintingStrategy = fingerprintingStrategy;
        this.estimationStrategy = estimationStrategy;
    }

    @Override
    public List<DoublePair> runExperiment() throws IOException {

        List<DoublePair> result = new ArrayList<>();

        TraceGenerator traceGenerator = Helpers.loadTraces(Constants.OFFLINE_TRACES, Constants.ONLINE_TRACES, Constants.OFFLINE_SIZE, Constants.ONLINE_SIZE);
        RadioMap radioMap = Helpers.train(traceGenerator, fingerprintingStrategy);
        Set<GeoPositionPair> results = Helpers.test(traceGenerator, estimationStrategy, radioMap);

        List<Double> errors = Helpers.computeErrors(results);

        int n = errors.size();
        for (int j = 0; j < n; j++) {
            Double fractile = ((j + 1.0) / n);
            Double error = errors.get(j);
            result.add(new DoublePair(fractile, error));
        }

        return result;
    }

    @Override
    public List<DoublePair> aggregateResults(List<DoublePair> results) {
        Map<Double, List<Double>> groups = results.stream()
                .collect(groupingBy(DoublePair::getFirst,
                        mapping(DoublePair::getSecond, toList())));

        return groups.entrySet()
                .stream()
                .map(e -> DoublePair.from(e.getValue().stream().mapToDouble(i -> i).average().getAsDouble(),
                        e.getKey()))
                .collect(toList());

    }
}
