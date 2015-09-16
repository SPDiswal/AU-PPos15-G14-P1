package solution;

import org.pi4.locutil.*;
import org.pi4.locutil.io.TraceGenerator;
import org.pi4.locutil.trace.*;
import solution.offline.*;
import solution.online.*;
import solution.utilities.Helpers;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class WiFiPositioningSystem
{
    private static final int OFFLINE_SIZE = 25;
    private static final int ONLINE_SIZE = 5;
    
    private static final String ONLINE_DATA = "data/MU.1.5meters.online.trace";
    private static final String OFFLINE_DATA = "data/MU.1.5meters.offline.trace";
    private static final String ACCESS_POINT_DATA = "data/MU.AP.positions";
    
    private static final String OUTPUT_PATH = "output/";
    private static final String EMPIRICAL_OUTPUT_NAME = "empirical-fp";
    private static final String MODEL_BASED_OUTPUT_NAME = "model-fp";
    private static final String NEAREST_NEIGHBOUR_OUTPUT_NAME = "-nn.txt";
    private static final String K_NEAREST_NEIGHBOUR_OUTPUT_NAME = "-knn.txt";
    private static final String SCORE_OUTPUT_NAME = "score.txt";
    
    public static void main(String[] args)
    {
        try
        {
            if (args.length >= 2)
            {
                if (args[0].toUpperCase().startsWith("-S"))
                {
                    generateErrorFunction(args[1]);
                }
                else
                {
                    trainAndTest(args);
                }
            }
            else
            {
                displayHelp();
            }
        }
        catch (IOException e)
        {
            // TODO
            e.printStackTrace();
        }
    }
    
    private static void trainAndTest(String[] args) throws IOException
    {
        String outputPath = OUTPUT_PATH;
        
        TraceGenerator traceGenerator = Helpers.loadTraces(OFFLINE_DATA, ONLINE_DATA, OFFLINE_SIZE, ONLINE_SIZE);
        Map<MACAddress, GeoPosition> accessPointPositions = Helpers.loadAccessPoints(ACCESS_POINT_DATA);
        
        FingerprintingStrategy fingerprintingStrategy;
        EstimationStrategy estimationStrategy;
        
        int argIndex = 0;
        
        if (args[argIndex].toUpperCase().startsWith("-E"))
        {
            fingerprintingStrategy = new EmpiricalStrategy();
            outputPath += EMPIRICAL_OUTPUT_NAME;
            argIndex += 1;
        }
        else if (args[argIndex].toUpperCase().startsWith("-M"))
        {
            fingerprintingStrategy = new ModelBasedStrategy(accessPointPositions,
                                                            3.415,
                                                            1,
                                                            -33.77,
                                                            Double.NEGATIVE_INFINITY /* TODO Read from arguments */);
            outputPath += MODEL_BASED_OUTPUT_NAME;
            argIndex += 1;
        }
        else
        {
            displayHelp();
            return;
        }
        
        if (args[argIndex].toUpperCase().startsWith("-NN"))
        {
            estimationStrategy = new NearestNeighbourStrategy();
            outputPath += NEAREST_NEIGHBOUR_OUTPUT_NAME;
        }
        else if (args[argIndex].toUpperCase().startsWith("-KNN"))
        {
            estimationStrategy = new KNearestNeighbourStrategy(Integer.parseInt(args[argIndex + 1]));
            outputPath += K_NEAREST_NEIGHBOUR_OUTPUT_NAME;
        }
        else
        {
            displayHelp();
            return;
        }
        
        RadioMap radioMap = Helpers.train(traceGenerator, fingerprintingStrategy);
        Set<GeoPositionPair> results = Helpers.test(traceGenerator, estimationStrategy, radioMap);
        
        writeResultsToFile(results, outputPath);
    }
    
    private static void writeResultsToFile(Set<GeoPositionPair> results, String outputPath) throws IOException
    {
        File file = new File(outputPath);
        
        try (FileWriter writer = new FileWriter(file))
        {
            String output = results.stream()
                                   .map(entry -> entry.getTruePosition().toStringWithoutOrientation()
                                                 + " " + entry.getEstimatedPosition().toStringWithoutOrientation())
                                   .collect(Collectors.joining(System.lineSeparator()));
            
            writer.write(output);
        }
    }
    
    private static void generateErrorFunction(String filename) throws IOException
    {
        File inputFile = new File(filename);
        Set<GeoPositionPair> results;

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile)))
        {
            results = reader.lines()
                            .filter(line -> !line.startsWith("#"))
                            .map(GeoPositionPair::parse)
                            .collect(toSet());
        }
        
        List<Double> errors = Helpers.computeErrors(results);
        
        File outputFile = new File(OUTPUT_PATH + SCORE_OUTPUT_NAME); // TODO Remove input file extension and concat with score output name.
        int n = errors.size();
        
        try (FileWriter writer = new FileWriter(outputFile))
        {
            for (int i = 0; i < n; i++)
            {
                writer.write(errors.get(i) + " " + ((i + 1.0) / n) + System.lineSeparator());
            }
        }
    }
    
    private static void displayHelp()
    {
        System.out.println("USAGE: WiFiPositioningSystem [e: empirical] ..."); // TODO
    }
}
