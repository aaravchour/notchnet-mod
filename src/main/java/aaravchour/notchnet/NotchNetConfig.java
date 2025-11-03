package aaravchour.notchnet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class NotchNetConfig {
    // Example config option
    public static boolean useAdvancedMode = true;
    public static String token = "";

    private static final File configFile = new File("config/notchnet.properties");

    public static void loadConfig() {
        Properties properties = new Properties();
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                token = properties.getProperty("token", "");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty("token", token);
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "NotchNet Config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
