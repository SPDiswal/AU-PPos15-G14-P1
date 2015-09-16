package solution.experiments;

import solution.Constants;
import solution.offline.EmpiricalStrategy;

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
    
    public ExperimentRunner()
    {
        this.experiments = new HashMap<>();
        
        add("distanceToSignalStrength-Empirical", new DistanceToSignalStrengthExperiment(new EmpiricalStrategy()));
        add("kToMedianError-Empirical", new KToMedianErrorExperiment(new EmpiricalStrategy()));
        
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
