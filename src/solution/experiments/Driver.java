package solution.experiments;

import org.pi4.locutil.GeoPosition;
import org.pi4.locutil.MACAddress;
import org.pi4.locutil.io.TraceGenerator;
import solution.offline.EmpiricalStrategy;
import solution.offline.FingerprintingStrategy;
import solution.offline.ModelBasedStrategy;
import solution.offline.RadioMap;
import solution.utilities.Helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Created by Thomas on 16-09-2015.
 */
public class Driver
{
    private static final int OFFLINE_SIZE = 25;
    private static final int ONLINE_SIZE = 5;

    private static final String ONLINE_DATA = "data/MU.1.5meters.online.trace";
    private static final String OFFLINE_DATA = "data/MU.1.5meters.offline.trace";
    private static final String ACCESS_POINT_DATA = "data/MU.AP.positions";
    private static final String OUTPUT_PATH = "experiments/";
    private static final String EMPIRICAL_OUTPUT = "empirical.txt";
    private static final String MODELBASED_OUTPUT = "model-based.txt";

    public static void main(String[] args) throws IOException {
        FingerprintingStrategy fingerprintingStrategy;
        String outputFileName;
        Map<MACAddress, GeoPosition> accessPointPositions = Helpers.loadAccessPoints(ACCESS_POINT_DATA);

        //fingerprintingStrategy = new EmpiricalStrategy();
        //outputFileName = OUTPUT_PATH + EMPIRICAL_OUTPUT;
        //signalStrengthsAndDistances(fingerprintingStrategy, outputFileName, accessPointPositions);

        fingerprintingStrategy = new ModelBasedStrategy(accessPointPositions, 3.415, 1, -33.77, Double.NEGATIVE_INFINITY);
        outputFileName = OUTPUT_PATH + MODELBASED_OUTPUT;
        signalStrengthsAndDistances(fingerprintingStrategy, outputFileName, accessPointPositions);
    }

    private static void signalStrengthsAndDistances(FingerprintingStrategy fingerprintingStrategy, String outputFileName, Map<MACAddress, GeoPosition> accessPointPositions) throws IOException {
        TraceGenerator traceGenerator = Helpers.loadTraces(OFFLINE_DATA, ONLINE_DATA, OFFLINE_SIZE, ONLINE_SIZE);

        RadioMap radioMap = Helpers.train(traceGenerator, fingerprintingStrategy);

        File outputFile = new File(outputFileName);

        try (FileWriter writer = new FileWriter(outputFile))
        {
            String output;
            for (GeoPosition position : radioMap.keySet())
            {
                for (MACAddress ap : radioMap.get(position).keySet())
                {
                    GeoPosition apPosition = accessPointPositions.get(ap);
                    if(apPosition == null){
                        continue;
                    }
                    Double distance = position.distance(apPosition);
                    Double signalStrength = radioMap.get(position).get(ap);
                    output = distance + " " + signalStrength;
                    writer.write(output + System.lineSeparator());
                }
            }
        }
    }
}
