package solution;

public class Constants
{
    // PARAMETERS FOR TRACE SAMPLING
    public static final int OFFLINE_SAMPLE_SIZE = 25;
    public static final int ONLINE_SAMPLE_SIZE = 5;
    
    // PATHS AND FILENAMES
    public static final String ONLINE_TRACES = "data/MU.1.5meters.online.trace";
    public static final String OFFLINE_TRACES = "data/MU.1.5meters.offline.trace";
    public static final String ACCESS_POINT_POSITIONS = "data/MU.AP.positions";
    
    public static final String OUTPUT_PATH = "output/";
    public static final String EXPERIMENT_OUTPUT_PATH = "output/experiments/";
    
    // PARAMETERS FOR MODEL-BASED FINGERPRINTING
    public static final double N = 3.415;
    public static final double D0 = 1;
    public static final double P_D0 = -33.77;
    public static final double UNHEARABLE_THRESHOLD = -120.0;
    
    // PARAMETERS FOR K NEAREST NEIGHBOURS
    public static final int COMMON_ACCESS_POINTS_THRESHOLD = 3;
}
