package solution.experiments;

import org.pi4.locutil.GeoPosition;
import org.pi4.locutil.MACAddress;
import org.pi4.locutil.io.TraceGenerator;
import solution.GeoPositionPair;
import solution.offline.EmpiricalStrategy;
import solution.offline.FingerprintingStrategy;
import solution.offline.ModelBasedStrategy;
import solution.offline.RadioMap;
import solution.online.EstimationStrategy;
import solution.online.NearestNeighbourStrategy;
import solution.utilities.Helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Created by Thomas on 16-09-2015.
 */
public class Driver
{
    private static final int OFFLINE_SIZE = 25;
    private static final int ONLINE_SIZE = 5;

    private static final String ONLINE_DATA = "data/MU.1.5meters.online.trace";
    private static final String OFFLINE_DATA = "data/MU.1.5meters.offline.trace";
    private static final String ACCESS_POINT_DATA = "data/MU.AP.positions";
    private static final String OUTPUT_PATH = "experiments/";
    private static final String EMPIRICAL_OUTPUT = "empirical.txt";
    private static final String MODELBASED_OUTPUT = "model-based.txt";
    private static final String EMPIRICAL_NN_ERROR_OUTPUT = "empirical-nn-error.txt";

    public static void main(String[] args) throws IOException {
        FingerprintingStrategy fingerprintingStrategy;
        EstimationStrategy estimationStrategy;
        String outputFileName;
        Map<MACAddress, GeoPosition> accessPointPositions = Helpers.loadAccessPoints(ACCESS_POINT_DATA);

//        fingerprintingStrategy = new EmpiricalStrategy();
//        outputFileName = OUTPUT_PATH + EMPIRICAL_OUTPUT;
//        signalStrengthsAndDistances(fingerprintingStrategy, outputFileName, accessPointPositions);
//
//        fingerprintingStrategy = new ModelBasedStrategy(accessPointPositions, 3.415, 1, -33.77, Double.NEGATIVE_INFINITY);
//        outputFileName = OUTPUT_PATH + MODELBASED_OUTPUT;
//        signalStrengthsAndDistances(fingerprintingStrategy, outputFileName, accessPointPositions);

        fingerprintingStrategy = new EmpiricalStrategy();
        estimationStrategy = new NearestNeighbourStrategy();
        outputFileName = OUTPUT_PATH + EMPIRICAL_NN_ERROR_OUTPUT;
        errorFunction(fingerprintingStrategy, estimationStrategy, outputFileName, accessPointPositions);
    }

    private static void errorFunction(FingerprintingStrategy fingerprintingStrategy, EstimationStrategy estimationStrategy, String outputFileName, Map<MACAddress, GeoPosition> accessPointPositions) throws IOException {
        File outputFile = new File(outputFileName);

        HashMap<Double, List<Double>> errorResults = new HashMap<>();

        try (FileWriter writer = new FileWriter(outputFile)) {
            for (int i = 0; i < 100; i++) {

                TraceGenerator traceGenerator = Helpers.loadTraces(OFFLINE_DATA, ONLINE_DATA, OFFLINE_SIZE, ONLINE_SIZE);
                RadioMap radioMap = Helpers.train(traceGenerator, fingerprintingStrategy);
                Set<GeoPositionPair> results = Helpers.test(traceGenerator, estimationStrategy, radioMap);

                List<Double> errors = Helpers.computeErrors(results);

                int n = errors.size();
                for (int j = 0; j < n; j++) {
                    Double fractile = ((j + 1.0) / n);
                    Double error = errors.get(j);
                    if(!errorResults.containsKey(fractile)){
                        errorResults.put(fractile, new ArrayList<>());
                    }
                    errorResults.get(fractile).add(error);
                }
            }
            String output = errorResults.entrySet().stream()
                    .map(entry -> entry.getValue().stream().mapToDouble(s -> s).average().getAsDouble() + "\t" + entry.getKey())
                    .collect(Collectors.joining(System.lineSeparator()));

            writer.write(output);
        }
    }

    private static void signalStrengthsAndDistances(FingerprintingStrategy fingerprintingStrategy, String outputFileName, Map<MACAddress, GeoPosition> accessPointPositions) throws IOException {

        File outputFile = new File(outputFileName);

        HashMap<Double, List<Double>> accResults = new HashMap<>();

        try (FileWriter writer = new FileWriter(outputFile))
        {
            for (int i = 0; i < 100; i++) {
                TraceGenerator traceGenerator = Helpers.loadTraces(OFFLINE_DATA, ONLINE_DATA, OFFLINE_SIZE, ONLINE_SIZE);
                RadioMap radioMap = Helpers.train(traceGenerator, fingerprintingStrategy);

                for (GeoPosition position : radioMap.keySet())
                {
                    for (MACAddress ap : radioMap.get(position).keySet())
                    {
                        GeoPosition apPosition = accessPointPositions.get(ap);
                        if(apPosition == null){
                            continue;
                        }
                        Double distance = position.distance(apPosition);
                        Double signalStrength = radioMap.get(position).get(ap);

                        if (!accResults.containsKey(distance)) {
                            accResults.put(distance, new ArrayList<>());
                        }
                        accResults.get(distance).add(signalStrength);
                    }
                }
            }

            String output = accResults.entrySet().stream()
                    .map(entry -> entry.getKey() + "\t" + entry.getValue().stream().mapToDouble(s -> s).average().getAsDouble())
                    .collect(Collectors.joining(System.lineSeparator()));

            writer.write(output);
        }
    }
}
