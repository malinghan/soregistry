package com.so.soregistry.health;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.so.soregistry.model.InstanceMeta;
import com.so.soregistry.service.RegistryService;

import lombok.extern.slf4j.Slf4j;

/**
 * health checker
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-04-19
 */
@Slf4j
public class SoHealthChecker implements HealthChecker {

    RegistryService registryService;

    public SoHealthChecker(RegistryService registryService) {
        this.registryService = registryService;
    }

    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    long timeout = 20_000;

    @Override
    public void start() {
        executor.scheduleWithFixedDelay(() -> {
            log.info(" ===> Health checker running...");
            long now = System.currentTimeMillis();
            // check all services
            registryService.getTimestamps().keySet().stream().forEach(x -> {
            long timestamp = registryService.getTimestamps().get(x);
            if (now - timestamp > timeout) {
                log.info(" ===> Health checker timeout for service {}", x);
                int index = x.indexOf("@");
                String service = x.substring(0, index); //service
                String url = x.substring(index+1); //??
                InstanceMeta instance = InstanceMeta.from(url);
                registryService.unregister(service, instance);
                registryService.getTimestamps().remove(x);
            }
            });
        }, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        executor.shutdown();
    }
}
