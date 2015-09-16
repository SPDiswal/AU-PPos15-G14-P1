package solution.experiments;

import java.util.*;

public interface ExperimentStrategy
{
    List<DoublePair> runExperiment();
    
    List<DoublePair> aggregateResults(Set<List<DoublePair>> results);
}
