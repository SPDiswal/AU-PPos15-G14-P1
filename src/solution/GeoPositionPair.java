package solution;

import org.pi4.locutil.GeoPosition;

/**
 * Created by Thomas on 15-09-2015.
 */
public class GeoPositionPair {
    public GeoPosition getEstimatedPosition() {
        return estimatedPosition;
    }

    public GeoPosition getTruePosition() {
        return truePosition;
    }

    private GeoPosition truePosition;
    private GeoPosition estimatedPosition;

    public GeoPositionPair(GeoPosition truePosition, GeoPosition estimatedPosition){
        this.truePosition = truePosition;
        this.estimatedPosition = estimatedPosition;
    }

    public static GeoPositionPair parse(String input)
    {
        String[] tokens = input.split("[)] [(]");
        GeoPosition pos1 = GeoPosition.parse(tokens[0].replace("(", ""));
        GeoPosition pos2 = GeoPosition.parse(tokens[1].replace(")", ""));
        return new GeoPositionPair(pos1, pos2);
    }
}
