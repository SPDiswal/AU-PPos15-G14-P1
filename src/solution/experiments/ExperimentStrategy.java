package solution.experiments;

import java.io.IOException;
import java.util.*;

public interface ExperimentStrategy
{
    List<DoublePair> runExperiment() throws IOException;
    
    List<DoublePair> aggregateResults(Set<List<DoublePair>> results);
}
