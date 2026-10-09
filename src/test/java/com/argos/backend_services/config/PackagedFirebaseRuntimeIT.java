package com.argos.backend_services.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.ZipFile;

class PackagedFirebaseRuntimeIT {
    @TempDir
    Path libraries;

    @Test
    void shouldFetchFirebaseCertificatesUsingOnlyPackagedDependencies() throws Exception {
        Path packagedApplication = Path.of("target/app.jar");
        Assumptions.assumeTrue(Files.exists(packagedApplication),
                "target/app.jar is generated only by the package phase");

        var classpath = new ArrayList<URL>();
        classpath.add(Path.of("target/test-classes").toUri().toURL());
        try (var jar = new ZipFile(packagedApplication.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.getName().startsWith("BOOT-INF/lib/") && entry.getName().endsWith(".jar")) {
                    Path destination = libraries.resolve(Path.of(entry.getName()).getFileName());
                    try (var source = jar.getInputStream(entry)) {
                        Files.copy(source, destination);
                    }
                    classpath.add(destination.toUri().toURL());
                }
            }
        }
        try (var loader = new URLClassLoader(classpath.toArray(URL[]::new),
                ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("com.argos.backend_services.config.FirebaseRuntimeProbe")
                    .getMethod("verifyCertificateFetch").invoke(null);
        }
    }
}
