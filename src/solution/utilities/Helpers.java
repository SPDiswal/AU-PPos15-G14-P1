package solution.utilities;

import org.pi4.locutil.*;
import org.pi4.locutil.io.TraceGenerator;
import org.pi4.locutil.trace.*;
import solution.GeoPositionPair;
import solution.offline.*;
import solution.online.EstimationStrategy;

import java.io.*;
import java.util.*;

import static java.util.stream.Collectors.*;

public class Helpers
{
    public static Map<GeoPosition, Set<TraceEntry>> groupEntriesByPosition(Set<TraceEntry> entries)
    {
        Map<GeoPosition, Set<TraceEntry>> result = new HashMap<>();
        
        Set<GeoPosition> allPositions = entries.stream()
                                               .map(TraceEntry::getGeoPosition)
                                               .collect(toSet());
        
        for (GeoPosition position : allPositions)
        {
            Set<TraceEntry> entriesAtPosition = entries.stream()
                                                       .filter(entry -> entry.getGeoPosition().equals(position))
                                                       .collect(toSet());
            
            result.put(position, entriesAtPosition);
        }
        
        return Collections.unmodifiableMap(result);
    }
    
    public static Set<MACAddress> getAvailableAccessPoints(Set<TraceEntry> entriesAtPosition)
    {
        Set<MACAddress> accessPoints = entriesAtPosition.stream()
                                                        .flatMap(entry -> entry.getSignalStrengthSamples().keySet()
                                                                               .stream())
                                                        .collect(toSet());
        
        return Collections.unmodifiableSet(accessPoints);
    }
    
    public static Map<MACAddress, Double> computeAverageSignalStrengthByAccessPoint(Set<TraceEntry> entriesAtPosition,
                                                                                    Set<MACAddress> accessPoints)
    {
        Map<MACAddress, Double> result = new HashMap<>();
        
        for (MACAddress accessPoint : accessPoints)
        {
            double averageSignalStrength = entriesAtPosition.stream()
                                                            .map(TraceEntry::getSignalStrengthSamples)
                                                            .filter(samples -> samples.containsKey(accessPoint))
                                                            .mapToDouble(samples -> samples
                                                                    .getAverageSignalStrength(accessPoint))
                                                            .average().getAsDouble();
            
            result.put(accessPoint, averageSignalStrength);
        }
        
        return Collections.unmodifiableMap(result);
    }
    
    public static <T> Set<T> intersection(Set<T> a, Set<T> b)
    {
        return a.stream().filter(b::contains).collect(toSet());
    }
    
    public static double median(Collection<Double> values)
    {
        List<Double> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);
        
        if (sortedValues.size() % 2 == 1)
        {
            return sortedValues.get((sortedValues.size() + 1) / 2 - 1);
        }
        else
        {
            double lower = sortedValues.get(sortedValues.size() / 2 - 1);
            double upper = sortedValues.get(sortedValues.size() / 2);
            
            return (lower + upper) / 2.0;
        }
    }
    
    /////
    
    private static Parser createParser(String path)
    {
        File file = new File(path);
        return new Parser(file);
    }
    
    public static TraceGenerator loadTraces(String offlineData,
                                            String onlineData,
                                            int offlineSize,
                                            int onlineSize) throws IOException
    {
        Parser offlineParser = createParser(offlineData);
        Parser onlineParser = createParser(onlineData);
        
        TraceGenerator traceGenerator = new TraceGenerator(offlineParser, onlineParser, offlineSize, onlineSize);
        
        traceGenerator.generate();
        return traceGenerator;
    }
    
    public static RadioMap train(TraceGenerator traceGenerator,
                                 FingerprintingStrategy fingerprintingStrategy) throws IOException
    {
        return fingerprintingStrategy.createRadioMap(new HashSet<>(traceGenerator.getOffline()));
    }
    
    public static Set<GeoPositionPair> test(TraceGenerator traceGenerator,
                                            EstimationStrategy estimationStrategy,
                                            RadioMap radioMap)
    {
        Set<GeoPositionPair> results = new HashSet<>();
        
        Set<TraceEntry> onlineEntries = new HashSet<>(traceGenerator.getOnline());
        
        for (TraceEntry entry : onlineEntries)
        {
            Map<MACAddress, Double> measurements = entry.getSignalStrengthSamples().keySet().stream()
                                                        .collect(toMap(key -> key,
                                                                       key -> entry.getSignalStrengthSamples()
                                                                                   .getAverageSignalStrength(key)));
            
            GeoPosition estimatedPosition = estimationStrategy.estimatePosition(radioMap, measurements);
            
            results.add(new GeoPositionPair(entry.getGeoPosition(), estimatedPosition));
        }
        
        return results;
    }
    
    public static Map<MACAddress, GeoPosition> loadAccessPoints(String path) throws IOException
    {
        File file = new File(path);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            return reader.lines()
                         .filter(line -> !line.startsWith("#"))
                         .map(line -> line.split(" "))
                         .collect(toMap(tokens -> MACAddress.parse(tokens[0]),
                                        tokens -> GeoPosition.parse(tokens[1] + " " + tokens[2] + " " + tokens[3])));
        }
    }
    
    public static List<Double> computeErrors(Set<GeoPositionPair> results)
    {
        return results.stream()
                      .map(entry -> entry.getTruePosition().distance(entry.getEstimatedPosition()))
                      .sorted()
                      .collect(toList());
    }
}
