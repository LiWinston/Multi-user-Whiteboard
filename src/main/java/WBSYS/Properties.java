package WBSYS;

import java.text.SimpleDateFormat;
import java.util.Date;


public class Properties {
    public final static boolean RepeatTextPreviewSending = false;

    public final static String WELLCOME_MESSAGE = "Welcoming for client is Join:";

    public static boolean isValidPort(String portNum) {
        try {
            return Integer.parseInt(portNum) >= 1024 && Integer.parseInt(portNum) <= 65535;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    public static String chatMessageFormat(String username, String message) {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String time = simpleDateFormat.format(date);
        return time + " - " + username + " : " + message;
    }

    public static String managerMessage(String message) {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        String time = simpleDateFormat.format(date);
        return time + " - " + message;
    }


}
