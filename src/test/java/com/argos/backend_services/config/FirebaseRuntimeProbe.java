package com.argos.backend_services.config;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.AuthErrorCode;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import java.util.Date;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class FirebaseRuntimeProbe {
    public static void verifyCertificateFetch() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        var transport = new MockHttpTransport.Builder().setLowLevelHttpResponse(
            new MockLowLevelHttpResponse().setStatusCode(200).setContent("{}")
            .setContentType("application/json")).build();
        var app = FirebaseApp.initializeApp(FirebaseOptions.builder()
            .setProjectId("diagnostic-project").setHttpTransport(transport)
            .setCredentials(GoogleCredentials.create(new AccessToken("unused", new Date())))
            .build(), "runtime-smoke");
        String header = "{\"alg\":\"RS256\",\"kid\":\"diagnostic-key\"}";
        String payload = "{\"aud\":\"diagnostic-project\",\"iss\":\"https://securetoken.google.com/diagnostic-project\",\"sub\":\"diagnostic-user\",\"iat\":" + now + ",\"exp\":" + (now + 3600) + "}";
        var encoder = Base64.getUrlEncoder().withoutPadding();
        String token = encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
            + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "."
            + encoder.encodeToString(new byte[256]);
        try {
            try {
                FirebaseAuth.getInstance(app).verifyIdToken(token);
                throw new AssertionError("Fake signature accepted");
            } catch (FirebaseAuthException e) {
                if (e.getAuthErrorCode() != AuthErrorCode.INVALID_ID_TOKEN) throw e;
                if (transport.getLowLevelHttpRequest() == null) throw new AssertionError("HTTP path not reached");
                
            }
        } finally { app.delete(); }
    }
}

