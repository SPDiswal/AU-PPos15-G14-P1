package solution.experiments;

import org.pi4.locutil.*;
import solution.Constants;
import solution.offline.*;
import solution.online.*;
import solution.utilities.Helpers;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ExperimentRunner
{
    public static void main(String[] args)
    {
        try
        {
            new ExperimentRunner().run();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private Map<String, ExperimentStrategy> experiments;
    
    public ExperimentRunner() throws IOException
    {
        this.experiments = new HashMap<>();
        Map<MACAddress, GeoPosition> accessPoints = Helpers.loadAccessPoints(Constants.ACCESS_POINT_POSITIONS);
    
        FingerprintingStrategy empirical = new EmpiricalStrategy();
        FingerprintingStrategy model = new ModelBasedStrategy(accessPoints);
        EstimationStrategy nearestNeighbour = new NearestNeighbourStrategy();
        EstimationStrategy threeNearestNeighbours = new KNearestNeighbourStrategy(3);
        
        add("distanceToSignalStrength-Empirical", new DistanceToSignalStrengthExperiment(empirical));
        add("distanceToSignalStrength-Model", new DistanceToSignalStrengthExperiment(model));
        add("cumulativeError-Empirical-NN", new ErrorFunctionExperiment(empirical, nearestNeighbour));
        add("cumulativeError-Empirical-KNN", new ErrorFunctionExperiment(empirical, threeNearestNeighbours));
        add("cumulativeError-Model-NN", new ErrorFunctionExperiment(model, nearestNeighbour));
        add("cumulativeError-Model-KNN", new ErrorFunctionExperiment(model, threeNearestNeighbours));
        add("kToMedianError-Empirical", new KToMedianErrorExperiment(empirical));
        add("kToMedianError-Model", new KToMedianErrorExperiment(model));
        
        this.experiments = Collections.unmodifiableMap(this.experiments);
    }
    
    private void add(String name, ExperimentStrategy experiment)
    {
        this.experiments.put(name, experiment);
    }
    
    public void run() throws IOException
    {
        for (String name : experiments.keySet())
        {
            System.out.println("PERFORMING " + name);
            
            try (FileWriter writer = new FileWriter(Constants.EXPERIMENT_OUTPUT_PATH + name + ".txt"))
            {
                List<DoublePair> results = new ArrayList<>();
                
                for (int i = 1; i <= 100; i++)
                {
                    results.addAll(experiments.get(name).runExperiment());
                    
                    if (i % 10 == 0)
                    {
                        System.out.println("Iteration " + i);
                    }
                }
                
                String output = experiments.get(name)
                                           .aggregateResults(results)
                                           .stream()
                                           .map(e -> e.getFirst() + "\t" + e.getSecond())
                                           .collect(Collectors.joining(System.lineSeparator()));
                
                writer.write(output);
                
                System.out.println("Finished.");
                System.out.println();
            }
        }
    }
}
