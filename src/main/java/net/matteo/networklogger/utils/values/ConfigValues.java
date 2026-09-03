package net.matteo.networklogger.utils.values;

public class ConfigValues {
    //Name
    public static String stringWriteIfContainsString = "Write file if chat message contains 'lag or 'ping'";
    public static String stringMessageEnabled = "Enable message";
    public static String stringMessageValue = "Trigger message if ping excess";

    public static String stringClearIfPingExcess = "clear if a player ping excess";
    public static String stringWriteAfterPingExcess = "Write once ping excess is over";
    public static String stringOnlyCaptureGlobalOver = "If player receiver is not null, only capture global data if player receiver ping is over";

    public static String stringOnlyCapturePlayerOver = "Only capture player data if player receiver ping is over";
    public static String stringSendMessage = "Send the following message";
    public static String stringConnectionStable = "If player started moving, consider their connection stable after X minutes";

    public static String stringDeleteFileOnExit = "Delete the profiler and payload folder on game exit";
    public static String stringIsModEnabled = "Whether the mod is considered enabled or not";

    //Value
    public static boolean valueWriteIfContains = false; //Set by config reader
    public static boolean valueWriteAfterPingExcess = false; //Set by config reader
    public static boolean valueMessageEnabled = false;

    public static boolean valueDeleteFileOnExit = true;
    public static boolean valueIsModEnabled = true;

    public static int valueClearIfPingExcess = 0; //Set by config reader
    public static int valueOnlyCapturePlayerOver = 0; //Set by config reader
    public static int valueOnlyCaptureGlobalOver = 0; //Set by config reader

    public static int valuePingExcess = 0;
    public static int valueConnectionStable = 180;

    public static String valueSendMessage = "";
}
