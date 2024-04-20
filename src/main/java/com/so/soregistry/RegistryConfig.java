package com.so.soregistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.so.soregistry.health.HealthChecker;
import com.so.soregistry.health.SoHealthChecker;
import com.so.soregistry.service.RegistryService;
import com.so.soregistry.service.SoRegistryService;

/**
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-04-19
 */
@Configuration
public class RegistryConfig {
    @Bean
    public RegistryService registryService() {
        return new SoRegistryService();
    }

        @Bean(initMethod = "start", destroyMethod = "stop")
        public HealthChecker healthChecker(@Autowired RegistryService registryService) {
            return new SoHealthChecker(registryService);
        }
}
