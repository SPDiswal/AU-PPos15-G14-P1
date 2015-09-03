package example;

import org.pi4.locutil.io.TraceGenerator;
import org.pi4.locutil.trace.*;

import java.io.*;

/**
 * Example of how to use LocUtil
 *
 * @author mikkelbk
 */
public class LocUtilExample
{
    public static void main(String[] args)
    {
        String offlinePath = "data/MU.1.5meters.offline.trace";
        String onlinePath = "data/MU.1.5meters.online.trace";
        
        // Constructs parsers of trace files.
        File offlineFile = new File(offlinePath);
        Parser offlineParser = new Parser(offlineFile);
        System.out.println("Offline File: " + offlineFile.getAbsoluteFile());
        
        File onlineFile = new File(onlinePath);
        Parser onlineParser = new Parser(onlineFile);
        System.out.println("Online File: " + onlineFile.getAbsoluteFile());
        
        try
        {
            int offlineSize = 25;
            int onlineSize = 5;
            
            TraceGenerator tg = new TraceGenerator(offlineParser, onlineParser, offlineSize, onlineSize);
            
            tg.generate();
            
            System.out.println("OFFLINE TRACES:");
            System.out.println();
            
            for (TraceEntry entry : tg.getOffline())
            {
                System.out.println(
                        entry.getGeoPosition().toStringWithoutOrientation() + ": " + entry.getSignalStrengthSamples()
                                                                                          .size());
            }
            
            System.out.println();
            System.out.println();
            System.out.println("ONLINE TRACES:");
            System.out.println();
            
            for (TraceEntry entry : tg.getOnline())
            {
                System.out.println(
                        entry.getGeoPosition().toStringWithoutOrientation() + ": " + entry.getSignalStrengthSamples()
                                                                                          .size());
            }
        }
        catch (NumberFormatException | IOException e)
        {
            e.printStackTrace();
        }
    }
}
