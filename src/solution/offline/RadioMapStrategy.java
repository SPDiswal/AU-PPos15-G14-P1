package solution.offline;

import org.pi4.locutil.trace.TraceEntry;

import java.util.Set;

public interface RadioMapStrategy
{
    RadioMap createRadioMap(Set<TraceEntry> entries);
}
