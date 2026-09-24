package com.argos.backend_services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendServicesApplication {

  public static void main(String[] args) {
    SpringApplication.run(BackendServicesApplication.class, args);
  }
}
