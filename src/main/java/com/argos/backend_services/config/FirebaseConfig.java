package com.argos.backend_services.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnProperty(name = "firebase.service-account-path")
public class FirebaseConfig {

    @Value("${firebase.service-account-path:#{null}}")
    private String serviceAccountPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials = resolveCredentials();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

        return FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        String json = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
        if (json != null && !json.isBlank()) {
            return GoogleCredentials.fromStream(
                    new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
            );
        }

        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            Resource resource = new DefaultResourceLoader()
                    .getResource(serviceAccountPath);
            return GoogleCredentials.fromStream(resource.getInputStream());
        }

        throw new IllegalStateException("Credenciais firebase não configuradas");
    }
}