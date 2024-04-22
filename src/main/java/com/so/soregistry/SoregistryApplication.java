package com.so.soregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SoRegistryConfigProperties.class)
public class SoregistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoregistryApplication.class, args);
    }

}
