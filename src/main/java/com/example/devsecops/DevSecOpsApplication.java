package com.example.devsecops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DevSecOpsApplication {
    public static void main(String[] args) {
        SpringApplication.run(DevSecOpsApplication.class, args);
    }
}
