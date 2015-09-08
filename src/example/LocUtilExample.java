package example;

import org.pi4.locutil.*;
import org.pi4.locutil.io.TraceGenerator;
import org.pi4.locutil.trace.*;
import solution.offline.*;
import solution.online.NearestNeighbourStrategy;
import solution.utilities.Helpers;

import java.io.*;
import java.util.*;

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
            
            RadioMap radioMap = new EmpiricalStrategy().createRadioMap(new HashSet<>(tg.getOffline()));
            
            System.out.println(radioMap.toString());
            
            Map<GeoPosition, Set<TraceEntry>> entriesByPosition = Helpers
                    .groupEntriesByPosition(new HashSet<>(tg.getOnline()));
            
            Set<GeoPosition> allPositions = entriesByPosition.keySet();
            
            System.out.println();
            System.out.println("ESTIMATES:");
            
            List<Double> errors = new ArrayList<>();
            
            for (GeoPosition truePosition : allPositions)
            {
                Set<TraceEntry> entriesAtPosition = entriesByPosition.get(truePosition);
                Set<MACAddress> accessPoints = Helpers.getAvailableAccessPoints(entriesAtPosition);
                
                Map<MACAddress, Double> measurements = Helpers
                        .computeAverageSignalStrengthByAccessPoint(entriesAtPosition,
                                                                   accessPoints);
                
                GeoPosition estimatedPosition = new NearestNeighbourStrategy().estimatePosition(radioMap, measurements);
    
                double error = truePosition.distance(estimatedPosition);
                errors.add(error);
                
                System.out.println(truePosition.toStringWithoutOrientation()
                                   + " -> " + estimatedPosition.toStringWithoutOrientation()
                                   + " (off by " + error + ")");
            }
            
            System.out.println();
            System.out.println("MEDIAN ERROR: " + Helpers.median(errors));
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
