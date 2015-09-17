package solution;

import org.pi4.locutil.*;
import org.pi4.locutil.io.TraceGenerator;
import solution.offline.*;
import solution.online.*;
import solution.utilities.Helpers;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

public class WiFiPositioningSystem
{
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
                trainAndTest(args);
            }
            else
            {
                displayHelp();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private static void trainAndTest(String[] args) throws IOException
    {
        String outputPath = Constants.OUTPUT_PATH;
        
        TraceGenerator traceGenerator = Helpers.loadTraces(Constants.OFFLINE_TRACES, Constants.ONLINE_TRACES,
                                                           Constants.OFFLINE_SIZE, Constants.ONLINE_SIZE);
        Map<MACAddress, GeoPosition> accessPointPositions = Helpers.loadAccessPoints(Constants.ACCESS_POINT_POSITIONS);
        
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
            fingerprintingStrategy = new ModelBasedStrategy(accessPointPositions);
            outputPath += MODEL_BASED_OUTPUT_NAME;
            argIndex += 1;
        }
        else if (args[argIndex].toUpperCase().startsWith("-S"))
        {
            generateErrorFunction(args[argIndex + 1]);
            return;
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
            argIndex += 1;
        }
        else if (args[argIndex].toUpperCase().startsWith("-KNN"))
        {
            try
            {
                estimationStrategy = new KNearestNeighbourStrategy(Integer.parseInt(args[argIndex + 1]));
                outputPath += K_NEAREST_NEIGHBOUR_OUTPUT_NAME;
                argIndex += 2;
            }
            catch (NumberFormatException e)
            {
                displayHelp();
                return;
            }
        }
        else
        {
            displayHelp();
            return;
        }
        
        RadioMap radioMap = Helpers.train(traceGenerator, fingerprintingStrategy);
        Set<GeoPositionPair> results = Helpers.test(traceGenerator, estimationStrategy, radioMap);
        
        writeResultsToFile(results, outputPath);
        
        if (args[argIndex].toUpperCase().startsWith("-S"))
        {
            generateErrorFunction(outputPath);
        }
        else
        {
            displayHelp();
        }
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
        
        String scoreFilename = filename.substring(0, filename.lastIndexOf("."))
                               + "-"
                               + SCORE_OUTPUT_NAME;
        
        File outputFile = new File(scoreFilename);
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
        System.out.println("USAGE: WiFiPositioningSystem\n"
                           + "[-e: empirical fingerprinting]\n"
                           + "[-m: model-based fingerprinting]\n"
                           + "[-nn: nearest neighbour estimation]\n"
                           + "[-knn K: K nearest neighbour estimation]\n"
                           + "[-s: compute score]\n"
                           + "[filename: output file to compute score from]");
    }
}
