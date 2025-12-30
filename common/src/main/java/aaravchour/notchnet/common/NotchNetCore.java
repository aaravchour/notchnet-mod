package aaravchour.notchnet.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotchNetCore {

    public static String askQuestion(String question) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("question", question);

        HttpURLConnection conn = (HttpURLConnection) new URL(CoreConfig.apiUrl + "/ask").openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(15_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String body = readResponse(conn);

        if (code != 200) {
            throw new IOException("Server returned " + code + ": " + body);
        }

        String answer = parseJsonField(body, "answer");
        if (answer == null) {
            throw new IOException("No 'answer' field in server response: " + body);
        }
        return answer.replace("\\n", "\n");
    }

    public static String readResponse(HttpURLConnection conn) throws IOException {
        InputStream stream = (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public static String parseJsonField(String json, String field) {
        if (json == null) return null;

        Pattern strPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher m = strPattern.matcher(json);
        if (m.find()) {
            return unescapeJsonString(m.group(1));
        }

        Pattern nonStrPattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*([^,}\\]]+)");
        m = nonStrPattern.matcher(json);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public static String unescapeJsonString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '/': sb.append('/'); i++; break;
                    case 'b': sb.append('\b'); i++; break;
                    case 'f': sb.append('\f'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'u':
                        if (i + 5 < s.length()) {
                            String hex = s.substring(i + 2, i + 6);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 5;
                            } catch (NumberFormatException ex) {
                                sb.append("\\u");
                                i++;
                            }
                        } else {
                            sb.append("\\u");
                            i++;
                        }
                        break;
                    default:
                        sb.append(next);
                        i++;
                        break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
