package dev.struggzard.sample.keycloakauth;

import io.javalin.Javalin;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Application entry point.
 */
public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    // see client id on KeyCloak Clients Settings section
    private static final String CLIENT_ID = "sample-client";

    // see client secret on KeyCloak Clients Credentials section (when client `Access Type` set to confidential)
    private static final String CLIENT_SECRET = "b39e2fed-447c-4506-bb80-0feb3c89db1a";

    // used to retrieve tokens data (access, refresh, etc)
    private static final String TOKEN_URL = "http://localhost:7001/auth/realms/sample-realm/protocol/openid-connect/token";

    // used to retrieve authorization code
    private static final String AUTH_URL = "http://localhost:7001/auth/realms/sample-realm/protocol/openid-connect/auth";

    // this app endpoint to handle authorization code and initiate token retrieval
    private static final String REDIRECT_URL = "http://localhost:7000/auth-check";

    // application entry point
    private static final String APP_HOME_URL = "http://localhost:7000/";

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> config.addStaticFiles("/public"));
        app.start(7000);

        app.get("/", ctx -> {
            String token = ctx.queryParam("token");

            // if authorization header missing redirecting into keyCloak
            if (token == null || token.equals("")) {
                URI uri = URI.create(AUTH_URL +
                        "?response_type=code" +
                        "&client_id=" + CLIENT_ID +
                        "&redirect_uri=" + REDIRECT_URL);

                logger.info("Missing HTTP Authorization header! Redirecting to: {}", uri);
                ctx.redirect(uri.toString(), 302);
                return;
            }
            ctx.result("Access token: "+ token);
        });

        app.get("/auth-check", ctx -> {
            String authCode = ctx.queryParam("code");
            if (authCode == null) {
                ctx.result("Error: unset authorization code!");
                return;
            }
            logger.info("Authorization Code {}", authCode);

            Map<String, String> payload = Map.of(
                    "grant_type", "authorization_code",
                    "code", authCode,
                    "redirect_uri", REDIRECT_URL
            );
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .POST(ofFormData(payload))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", composeBase64Credentials(CLIENT_ID, CLIENT_SECRET))
                    .build();

            var client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.info("Token successfully acquired. Redirecting to home page: {}", APP_HOME_URL);
                ctx.res.addHeader("token_data", response.body());
                ctx.redirect(APP_HOME_URL + "?token=" + extractAccessToken(response.body()));
            } else {
                ctx.result("An error occurred!");
            }
        });
    }

    private static String extractAccessToken(String body) {
        JSONObject tokenData = new JSONObject(body);
        return tokenData.getString("access_token");
    }

    // with some third-party HTTP networking lib this boilerplate likely would be unnecessary
    public static HttpRequest.BodyPublisher ofFormData(Map<String, String> data) {
        var builder = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

    private static String composeBase64Credentials(String user, String pass) {
        String credentials = user + ":" + pass;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
