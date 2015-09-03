package solution.offline;

import org.pi4.locutil.trace.TraceEntry;

import java.util.List;

public interface RadioMapStrategy
{
    RadioMap createRadioMap(List<TraceEntry> entries);
}
