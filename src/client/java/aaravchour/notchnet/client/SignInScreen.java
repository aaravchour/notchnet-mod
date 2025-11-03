package aaravchour.notchnet.client;

import aaravchour.notchnet.NotchNetConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SignInScreen extends Screen {

    private TextFieldWidget usernameField;
    private TextFieldWidget passwordField;

    public SignInScreen() {
        super(Text.literal("Sign In"));
    }

    @Override
    protected void init() {
        super.init();
        this.usernameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 50, 200, 20, Text.literal("Username"));
        this.passwordField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, this.height / 2 - 20, 200, 20, Text.literal("Password"));
        this.addDrawableChild(this.usernameField);
        this.addDrawableChild(this.passwordField);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Sign In"), button -> {
            signIn(this.usernameField.getText(), this.passwordField.getText());
        }).dimensions(this.width / 2 - 100, this.height / 2 + 10, 200, 20).build());
    }

    private void signIn(String username, String password) {
        try {
            URL url = new URL("http://localhost:8000/signin");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JsonObject json = new JsonObject();
            json.addProperty("username", username);
            json.addProperty("password", password);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(new Gson().toJson(json).getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    JsonObject responseJson = new Gson().fromJson(response.toString(), JsonObject.class);
                    String token = responseJson.get("token").getAsString();
                    NotchNetConfig.token = token;
                    NotchNetConfig.saveConfig();
                    this.client.setScreen(null);
                }
            } else {
                // TODO: Handle failed sign in
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 80, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
