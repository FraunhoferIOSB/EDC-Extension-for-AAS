package de.fraunhofer.iosb.app.util;

public class OSUtil {

    public static final String LOCALHOST_IP = "127.0.0.1";
    public static final String LOCALHOST = "localhost";
    private static final String OS_NAME = "os.name";
    private static final String WINDOWS = "Windows";

    private OSUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String getLocalhostAddress() {
        return System.getProperty(OS_NAME).startsWith(WINDOWS) ? LOCALHOST_IP : LOCALHOST;
    }
}

