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

public class PP1
{
    private static final String EMPIRICAL_OUTPUT_NAME = "empirical-fp";
    private static final String MODEL_BASED_OUTPUT_NAME = "model-fp";
    private static final String NEAREST_NEIGHBOUR_OUTPUT_NAME = "-nn.txt";
    private static final String K_NEAREST_NEIGHBOUR_OUTPUT_NAME = "-knn.txt";
    
    private static final String SCORE_OUTPUT_NAME = "-score.txt";
    
    public static void main(String[] args)
    {
        try
        {
            trainAndTest(args);
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
                                                           Constants.OFFLINE_SAMPLE_SIZE, Constants.ONLINE_SAMPLE_SIZE);
        Map<MACAddress, GeoPosition> accessPointPositions = Helpers.loadAccessPoints(Constants.ACCESS_POINT_POSITIONS);
        
        FingerprintingStrategy fingerprintingStrategy;
        EstimationStrategy estimationStrategy;
        
        int argIndex = 0;
        
        if (ensureArgs(args, argIndex, 1) && args[argIndex].toUpperCase().startsWith("--E"))
        {
            argIndex += 1;
            fingerprintingStrategy = new EmpiricalStrategy();
            outputPath += EMPIRICAL_OUTPUT_NAME;
        }
        else if (ensureArgs(args, argIndex, 1) && args[argIndex].toUpperCase().startsWith("--M"))
        {
            argIndex += 1;
            
            if (ensureArgs(args, argIndex, 3) && !args[argIndex].toUpperCase().startsWith("--"))
            {
                try
                {
                    double n = Double.parseDouble(args[argIndex]);
                    double d0 = Double.parseDouble(args[argIndex + 1]);
                    double p_d0 = Double.parseDouble(args[argIndex + 2]);
                    
                    fingerprintingStrategy = new ModelBasedStrategy(accessPointPositions, n, d0, p_d0,
                                                                    Constants.UNHEARABLE_THRESHOLD);
                    argIndex += 3;
                }
                catch (NumberFormatException e)
                {
                    System.out.println("Model parameters must be numbers.");
                    return;
                }
            }
            else
            {
                fingerprintingStrategy = new ModelBasedStrategy(accessPointPositions);
            }
            
            outputPath += MODEL_BASED_OUTPUT_NAME;
        }
        else if (ensureArgs(args, argIndex, 2) && args[argIndex].toUpperCase().startsWith("--S"))
        {
            generateErrorFunction(args[argIndex + 1]);
            return;
        }
        else
        {
            displayHelp();
            return;
        }
        
        if (ensureArgs(args, argIndex, 1) && args[argIndex].toUpperCase().startsWith("--NN"))
        {
            argIndex += 1;
            estimationStrategy = new NearestNeighbourStrategy();
            outputPath += NEAREST_NEIGHBOUR_OUTPUT_NAME;
        }
        else if (ensureArgs(args, argIndex, 2) && args[argIndex].toUpperCase().startsWith("--KNN"))
        {
            try
            {
                int k = Integer.parseInt(args[argIndex + 1]);
                
                if (k >= 1)
                {
                    estimationStrategy = new KNearestNeighbourStrategy(k);
                    argIndex += 2;
                    outputPath += K_NEAREST_NEIGHBOUR_OUTPUT_NAME;
                }
                else
                {
                    System.out.println("K must be a positive integer.");
                    return;
                }
            }
            catch (NumberFormatException e)
            {
                System.out.println("K must be a positive integer.");
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
        
        if (ensureArgs(args, argIndex, 1) && args[argIndex].toUpperCase().startsWith("--S"))
        {
            generateErrorFunction(outputPath);
        }
    }
    
    private static void writeResultsToFile(Set<GeoPositionPair> results, String outputPath) throws IOException
    {
        File file = new File(outputPath);
        file.getParentFile().mkdirs();
        
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
        
        String scoreFilename = filename.substring(0, filename.lastIndexOf(".")) + SCORE_OUTPUT_NAME;
        
        File outputFile = new File(scoreFilename);
        outputFile.getParentFile().mkdirs();
        
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
        System.out.println("USAGE: PP1\n"
                           + "    --e: empirical fingerprinting\n"
                           + "    --m: model-based fingerprinting\n"
                           + "    --m n d0 p_d0: model-based fingerprinting with custom parameters\n"
                           + "    --nn: nearest neighbour estimation\n"
                           + "    --knn K: K nearest neighbour estimation\n"
                           + "    --s: compute score from current context\n"
                           + "    --s filename: compute score from output file");
    }
    
    private static boolean ensureArgs(String[] args, int argIndex, int argsNeeded)
    {
        return args.length - argIndex >= argsNeeded;
    }
}
