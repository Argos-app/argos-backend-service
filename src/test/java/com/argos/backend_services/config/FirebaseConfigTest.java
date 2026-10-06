package com.argos.backend_services.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseConfigTest {
    @Test
    void shouldInitializeUsingJsonWithoutServiceAccountPath() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String privateKey = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        String json = new ObjectMapper().writeValueAsString(Map.of(
                "type", "service_account",
                "project_id", "test-project",
                "private_key_id", "test-key",
                "private_key", privateKey,
                "client_email", "test@test-project.iam.gserviceaccount.com",
                "client_id", "123456789",
                "token_uri", "https://oauth2.googleapis.com/token"));

        new ApplicationContextRunner().withUserConfiguration(FirebaseConfig.class)
                .withPropertyValues("FIREBASE_SERVICE_ACCOUNT_JSON=" + json)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FirebaseApp app = context.getBean(FirebaseApp.class);
                    try {
                        assertThat(app.getOptions().getProjectId()).isEqualTo("test-project");
                        assertThat(FirebaseAuth.getInstance(app)).isNotNull();
                    } finally {
                        app.delete();
                    }
                });
    }

    @Test
    void shouldFailAtStartupWhenCredentialsAreMissing() {
        new ApplicationContextRunner().withUserConfiguration(FirebaseConfig.class)
                .run(context -> assertThat(context).hasFailed());
    }
}
