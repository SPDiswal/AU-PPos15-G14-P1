package solution.offline;

import org.pi4.locutil.*;

import java.util.*;

public class RadioMap extends HashMap<GeoPosition, Map<MACAddress, Double>>
{
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("RadioMap").append(System.lineSeparator())
                                                              .append("{");
        
        int count = size();
        
        for (GeoPosition position : keySet())
        {
            sb.append(System.lineSeparator());
            sb.append("\t").append(position.toStringWithoutOrientation()).append(" =");
            
            Map<MACAddress, Double> samples = get(position);
            sb.append(System.lineSeparator()).append("\t").append("{").append(System.lineSeparator());
            
            for (MACAddress accessPoint : samples.keySet())
            {
                sb.append("\t").append("\t").append(accessPoint.toString())
                  .append("=").append(samples.get(accessPoint).toString()).append(System.lineSeparator());
            }
            
            count--;
            sb.append("\t").append(count == 0 ? "}" : "},");
        }
        
        sb.append(System.lineSeparator()).append("}");
        
        return sb.toString();
    }
}
