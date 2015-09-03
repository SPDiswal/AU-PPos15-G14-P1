package example;

import org.pi4.locutil.io.TraceGenerator;
import org.pi4.locutil.trace.*;
import solution.offline.EmpiricalStrategy;

import java.io.*;

/**
 * Example of how to use LocUtil
 *
 * @author mikkelbk
 */
public class LocUtilExample
{
    public static final int OFFLINE_SIZE = 25;
    public static final int ONLINE_SIZE = 5;
    
    public static void main(String[] args)
    {
        Parser offlineParser = createParser("data/MU.1.5meters.offline.trace");
        Parser onlineParser = createParser("data/MU.1.5meters.online.trace");
        
        try
        {
            TraceGenerator tg = new TraceGenerator(offlineParser, onlineParser, OFFLINE_SIZE, ONLINE_SIZE);
            
            tg.generate();
            
            System.out.println("OFFLINE TRACES:");
            System.out.println();
            
            tg.getOffline().forEach(example.LocUtilExample::printTraceEntry);
            
            System.out.println();
            System.out.println();
            System.out.println("ONLINE TRACES:");
            System.out.println();
            
            tg.getOnline().forEach(example.LocUtilExample::printTraceEntry);
    
            System.out.println(new EmpiricalStrategy().createRadioMap(tg.getOffline()).toString());
        }
        catch (NumberFormatException | IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private static Parser createParser(String path)
    {
        File file = new File(path);
        return new Parser(file);
    }
    
    private static void printTraceEntry(TraceEntry entry)
    {
        System.out.println(entry.getGeoPosition().toStringWithoutOrientation() + ": "
                           + entry.getSignalStrengthSamples().size());
    }
}
