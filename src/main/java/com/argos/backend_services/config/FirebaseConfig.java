package com.argos.backend_services.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("!test")
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account-path:#{null}}")
    private String serviceAccountPath;

    @Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}")
    private String serviceAccountJson;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials = resolveCredentials();

        if (!(credentials instanceof ServiceAccountCredentials serviceAccount)) {
            throw new IllegalStateException("Firebase exige credenciais de uma service account");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(serviceAccount.getProjectId())
                .build();

        log.info("firebase_initialized projectId={} credentialSource={}",
                serviceAccount.getProjectId(),
                serviceAccountJson.isBlank() ? "file" : "environment_json");
        return FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        String json = serviceAccountJson;
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
