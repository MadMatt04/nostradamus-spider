package si.mkejzar.ns;

/**
 * @author matijak
 * @since 14/06/16
 */
public class MatchesData {

    private MatchesData() {
    }

    public static final int NORMAL_MATCH_WORTH = 3;
    public static final int DOUBLE_MATCH_WORTH = NORMAL_MATCH_WORTH * 2;

    public static final int NORMAL_MATCHES = 36;
    public static final int DOUBLE_MATCHES = 10;

    public static final int TOTAL_POINTS = NORMAL_MATCHES * NORMAL_MATCH_WORTH + DOUBLE_MATCHES * DOUBLE_MATCH_WORTH;
    public static final int TOTAL_MATCHES = NORMAL_MATCHES + DOUBLE_MATCHES;

}
