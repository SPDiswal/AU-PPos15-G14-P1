package example;

import org.pi4.locutil.GeoPosition;
import org.pi4.locutil.MACAddress;
import org.pi4.locutil.io.TraceGenerator;
import org.pi4.locutil.trace.Parser;
import org.pi4.locutil.trace.TraceEntry;
import solution.offline.ModelBasedStrategy;
import solution.offline.RadioMap;
import solution.online.KNearestNeighbourStrategy;
import solution.online.NearestNeighbourStrategy;
import solution.utilities.Helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Example of how to use LocUtil
 *
 * @author mikkelbk
 */
public class LocUtilExample {
    public static final int OFFLINE_SIZE = 25;
    public static final int ONLINE_SIZE = 5;

    public static void main(String[] args) {
        Parser offlineParser = createParser("data/MU.1.5meters.offline.trace");
        Parser onlineParser = createParser("data/MU.1.5meters.online.trace");

        try {
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

            Map<MACAddress, GeoPosition> accessPointPositions = parseAccessPointPositions("data/MU.AP.positions");
            RadioMap radioMap = new ModelBasedStrategy(accessPointPositions, 3.415, 1, -33.77, Double.NEGATIVE_INFINITY).createRadioMap(new HashSet<>(tg.getOffline()));
            
            System.out.println(radioMap.toString());

            Map<GeoPosition, Set<TraceEntry>> entriesByPosition = Helpers
                    .groupEntriesByPosition(new HashSet<>(tg.getOnline()));

            Set<GeoPosition> allPositions = entriesByPosition.keySet();

            System.out.println();
            System.out.println("ESTIMATES:");

            List<Double> firstErrors = new ArrayList<>();
            List<Double> secondErrors = new ArrayList<>();
            List<Double> thirdErrors = new ArrayList<>();
            List<Double> fourthErrors = new ArrayList<>();
            List<Double> fifthErrors = new ArrayList<>();
            
            for (GeoPosition truePosition : allPositions) {
                Set<TraceEntry> entriesAtPosition = entriesByPosition.get(truePosition);
                Set<MACAddress> accessPoints = Helpers.getAvailableAccessPoints(entriesAtPosition);

                Map<MACAddress, Double> measurements = Helpers
                        .computeAverageSignalStrengthByAccessPoint(entriesAtPosition,
                                accessPoints);

                GeoPosition firstEstimatedPosition = new NearestNeighbourStrategy().estimatePosition(radioMap, measurements);
                GeoPosition secondEstimatedPosition = new KNearestNeighbourStrategy(2).estimatePosition(radioMap, measurements);
                GeoPosition thirdEstimatedPosition = new KNearestNeighbourStrategy(3).estimatePosition(radioMap, measurements);
                GeoPosition fourthEstimatedPosition = new KNearestNeighbourStrategy(4).estimatePosition(radioMap, measurements);
                GeoPosition fifthEstimatedPosition = new KNearestNeighbourStrategy(5).estimatePosition(radioMap, measurements);

                firstErrors.add(truePosition.distance(firstEstimatedPosition));
                secondErrors.add(truePosition.distance(secondEstimatedPosition));
                thirdErrors.add(truePosition.distance(thirdEstimatedPosition));
                fourthErrors.add(truePosition.distance(fourthEstimatedPosition));
                fifthErrors.add(truePosition.distance(fifthEstimatedPosition));

//                System.out.println(truePosition.toStringWithoutOrientation()
//                                   + " -> " + firstEstimatedPosition.toStringWithoutOrientation()
//                                   + " (off by " + firstError + ")");
            }

            System.out.println();
            System.out.println("    NN MEDIAN ERROR: " + Helpers.median(firstErrors));
            System.out.println("KNN(2) MEDIAN ERROR: " + Helpers.median(secondErrors));
            System.out.println("KNN(3) MEDIAN ERROR: " + Helpers.median(thirdErrors));
            System.out.println("KNN(4) MEDIAN ERROR: " + Helpers.median(fourthErrors));
            System.out.println("KNN(5) MEDIAN ERROR: " + Helpers.median(fifthErrors));
//            System.out.println();
//            System.out.println("GAIN: " + Helpers.median(firstErrors) / Helpers.median(secondErrors));
        } catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
    }

    private static Parser createParser(String path) {
        File file = new File(path);
        return new Parser(file);
    }

    private static void printTraceEntry(TraceEntry entry) {
        System.out.println(entry.getGeoPosition().toStringWithoutOrientation() + ": "
                + entry.getSignalStrengthSamples().size());
    }

    private static Map<MACAddress, GeoPosition> parseAccessPointPositions(String path) throws IOException {
        File file = new File(path);

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;

        Map<MACAddress, GeoPosition> result = new HashMap<>();

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#")) continue;
            String[] tokens = line.split(" ");

            MACAddress accessPoint = MACAddress.parse(tokens[0]);
            GeoPosition position = GeoPosition.parse(tokens[1] + " " + tokens[2] + " " + tokens[3]);

            result.put(accessPoint, position);
        }

        return result;
    }
}
