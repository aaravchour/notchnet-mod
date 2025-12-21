package aaravchour.notchnet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class NotchNetConfig {
    public static String apiUrl = "http://localhost:8000";
    public static boolean autoScanMods = true;
    public static boolean useAdvancedMode = true;

    private static final File configFile = new File("config/notchnet.properties");

    public static void loadConfig() {
        Properties properties = new Properties();
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
                apiUrl = fixUrl(properties.getProperty("apiUrl", "http://localhost:8000").trim());
                
                String autoScan = properties.getProperty("autoScanMods");
                if (autoScan != null) {
                    autoScanMods = Boolean.parseBoolean(autoScan.trim());
                }
                
                String advanced = properties.getProperty("useAdvancedMode");
                if (advanced != null) {
                    useAdvancedMode = Boolean.parseBoolean(advanced.trim());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String fixUrl(String url) {
        if (url == null || url.isEmpty()) return "http://localhost:8000";
        
        String fixed = url.trim();
        if (!fixed.startsWith("http://") && !fixed.startsWith("https://")) {
            fixed = "http://" + fixed;
        }

        // Handle 0.0.0.0 -> 127.0.0.1 and missing port 8000
        try {
            java.net.URL u = new java.net.URL(fixed);
            String host = u.getHost();
            int port = u.getPort();
            String protocol = u.getProtocol();

            if (host.equals("0.0.0.0")) {
                host = "127.0.0.1";
            }

            if (port == -1) {
                port = 8000;
            }

            fixed = protocol + "://" + host + ":" + port;
        } catch (Exception ignored) {
            // If URL parsing fails, just return as is (already has protocol)
        }

        return fixed;
    }

    public static void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty("apiUrl", apiUrl);
        properties.setProperty("autoScanMods", String.valueOf(autoScanMods));
        properties.setProperty("useAdvancedMode", String.valueOf(useAdvancedMode));
        
        File dir = configFile.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            properties.store(fos, "NotchNet Config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
