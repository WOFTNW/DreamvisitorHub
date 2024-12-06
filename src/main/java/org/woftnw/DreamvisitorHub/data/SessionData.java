package org.woftnw.DreamvisitorHub.data;

import java.util.ArrayList;
import java.util.List;

public class SessionData {

    /**
     * This class stores data that does not need to be stored across restarts.
     * This data should be changed only by function classes because changes may need to be reported to plugin.
     */

    public static int maxPlayerCount;
    public static int playerCount;
    public static double tps;
    public static double mspt;
    public static boolean restart;
    public static Boolean newResourcePack = null;
    public static boolean autoRestart;
    public static List<String> consoleLog = new ArrayList<>();
}
