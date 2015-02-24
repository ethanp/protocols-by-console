package server.util;

/**
 * Ethan Petuchowski 2/22/15
 */
public class Common {
    public static final int LOW_PORT = 3000;
    public static final String LOCALHOST = "0.0.0.0";
    public static final int MILLI_SEC = 1_000;

    public static String afterSpace(String cmd) {
        return cmd.substring(cmd.indexOf(' ')+1, cmd.length());
    }

    public static int MY_PORT = -1;
}
