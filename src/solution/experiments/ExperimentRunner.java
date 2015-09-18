package solution.experiments;

import org.pi4.locutil.*;
import solution.Constants;
import solution.offline.*;
import solution.online.*;
import solution.utilities.Helpers;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

public class ExperimentRunner
{
    private static final int NUMBER_OF_ITERATIONS = 200;
    
    public static void main(String[] args)
    {
        try
        {
            new ExperimentRunner().run();
        }
        catch (IOException | InterruptedException | ExecutionException e)
        {
            e.printStackTrace();
        }
    }
    
    private List<String> experimentNames;
    private List<ExperimentStrategy> experiments;
    
    public ExperimentRunner() throws IOException
    {
        experiments = new ArrayList<>();
        experimentNames = new ArrayList<>();
        
        setUpExperiments();
    }
    
    private void setUpExperiments() throws IOException
    {
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
        add("onlineSamplesToMedianError-Empirical-NN",
            new OnlineSamplesToMedianErrorExperiment(empirical, nearestNeighbour));
        add("onlineSamplesToMedianError-Empirical-KNN",
            new OnlineSamplesToMedianErrorExperiment(empirical, threeNearestNeighbours));
        add("onlineSamplesToMedianError-Model-NN",
            new OnlineSamplesToMedianErrorExperiment(model, nearestNeighbour));
        add("onlineSamplesToMedianError-Model-KNN",
            new OnlineSamplesToMedianErrorExperiment(model, threeNearestNeighbours));
    }
    
    private void add(String name, ExperimentStrategy experiment)
    {
        this.experimentNames.add(name);
        this.experiments.add(experiment);
    }
    
    public void run() throws IOException, ExecutionException, InterruptedException
    {
        for (int i = 0; i < experiments.size(); i++)
        {
            String name = experimentNames.get(i);
            ExperimentStrategy experiment = experiments.get(i);
            
            //                 Collection<DoublePair> results = runSequentially(name, experiment);
            Collection<DoublePair> results = runInParallel(name, experiment);
            
            String outputFilename = Constants.EXPERIMENT_OUTPUT_PATH + System.currentTimeMillis() + "-" + name + ".txt";
            
            File file = new File(outputFilename);
            file.getParentFile().mkdirs();
            
            try (FileWriter writer = new FileWriter(file))
            {
                writer.write(getAggregatedResults(experiment, results));
            }
        }
    }
    
    private Collection<DoublePair> runSequentially(String name, ExperimentStrategy experiment) throws IOException
    {
        System.out.print("[" + name + "] Initialising              \r");
        Collection<DoublePair> results = new ArrayList<>();
        long startTime = System.nanoTime();
        
        for (int j = 1; j <= NUMBER_OF_ITERATIONS; j++)
        {
            results.addAll(experiment.runExperiment());
            
            long timeSpent = Math.round((System.nanoTime() - startTime) / 1000000000.0);
            long completion = Math.round(100.0 * j / NUMBER_OF_ITERATIONS);
            System.out.print("[" + name + "] " + completion + "% (" + timeSpent + " s)              \r");
        }
        
        long timeSpent = Math.round((System.nanoTime() - startTime) / 1000000000.0);
        System.out.println("[" + name + "] Done in " + timeSpent + " s              ");
        return results;
    }
    
    private Collection<DoublePair> runInParallel(String name, ExperimentStrategy experiment) throws
                                                                                             ExecutionException,
                                                                                             InterruptedException
    {
        System.out.print("[" + name + "] Initialising              \r");
        Collection<DoublePair> results = new ConcurrentLinkedQueue<>();
        AtomicInteger completedIterations = new AtomicInteger(0);
        int processorsToUse = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
        long startTime = System.nanoTime();
        
        ForkJoinPool forkJoinPool = new ForkJoinPool(processorsToUse);
        forkJoinPool.submit(() -> {
            IntStream.rangeClosed(1, NUMBER_OF_ITERATIONS).parallel().forEach(j -> {
                try
                {
                    results.addAll(experiment.runExperiment());
                    
                    long timeSpent = Math.round((System.nanoTime() - startTime) / 1000000000.0);
                    long completion = Math.round(100.0 * completedIterations.incrementAndGet()
                                                 / NUMBER_OF_ITERATIONS);
                    System.out.print("[" + name + "] " + completion + "% (" + timeSpent + " s)              \r");
                }
                catch (IOException e)
                {
                    System.out.println("[" + name + "] FAILED: " + e.getMessage() + "              ");
                }
            });
            
            long timeSpent = Math.round((System.nanoTime() - startTime) / 1000000000.0);
            System.out.println("[" + name + "] Done in " + timeSpent + " s              ");
        }).get();
        
        return results;
    }
    
    private String getAggregatedResults(ExperimentStrategy experiment, Collection<DoublePair> results)
    {
        return experiment.aggregateResults(results)
                         .stream()
                         .map(e -> e.getFirst() + "\t" + e.getSecond())
                         .collect(Collectors.joining(System.lineSeparator()));
    }
}
