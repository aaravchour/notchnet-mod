package aaravchour.notchnet.common;

import java.net.MalformedURLException;
import java.net.URL;

public class CoreConfig {
    public static String apiUrl = "http://localhost:8000";

    public static String fixUrl(String url) {
        if (url == null || url.isEmpty()) return "http://localhost:8000";
        
        String fixed = url.trim();
        if (!fixed.startsWith("http://") && !fixed.startsWith("https://")) {
            fixed = "http://" + fixed;
        }

        try {
            URL u = new URL(fixed);
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
        } catch (MalformedURLException ignored) {
        }

        return fixed;
    }
}
