package com.so.soregistry.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.so.soregistry.cluster.Snapshot;
import com.so.soregistry.model.InstanceMeta;

import lombok.extern.slf4j.Slf4j;

/**
 * 注册服务实现
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-04-19
 */
@Slf4j
public class SoRegistryService implements RegistryService {

    private final static MultiValueMap<String, InstanceMeta> REGISTRY = new LinkedMultiValueMap<>(); //存储服务实例
    private final static Map<String, Long> VERSIONS = new ConcurrentHashMap<>(); //存储服务最新版本号
    private final static Map<String, Long>  TIMESTAMPS = new ConcurrentHashMap<>(); //store service timestamps
    public final static AtomicLong VERSION = new AtomicLong(0); //版本号生成器

    public Map<String, Long> getTimestamps() {
        return TIMESTAMPS;
    }

    @Override
    public InstanceMeta register(String service, InstanceMeta instance) {
       List<InstanceMeta> instances = REGISTRY.get(service);
       if (instances != null && !instances.isEmpty() && instances.contains(instance)) {
           log.info("====> instance {} already registered", instance.toUrl());
           instance.setStatus(true);
           return instance;
       }
       log.info("====> register instance {}", instance.toUrl());
       REGISTRY.add(service, instance);
       instance.setStatus(true);
       renew(instance, service);
       VERSIONS.put(service, VERSION.incrementAndGet());
       return instance;
    }

    @Override
    public InstanceMeta unregister(String service, InstanceMeta instance) {
        List<InstanceMeta> instances = REGISTRY.get(service);
        if (instances == null || instances.isEmpty()) {
            log.info("====> unregister instance is empty");
            return null;
        }
        log.info("====> unregister instance {}", instance.toUrl());
        //metas.remove(instance); //无法删除
        instances.removeIf(m -> m.equals(instance));
        instance.setStatus(false);
        renew(instance, service);
        VERSIONS.put(service, VERSION.incrementAndGet()); //新增服务版本号
        return instance;
    }

    @Override
    public List<InstanceMeta> getAllInstances(String service) {
        return REGISTRY.get(service);
    }

    @Override
    public long renew(InstanceMeta instance, String... services) {
        long now = System.currentTimeMillis();
        for (String service : services) {
            // service + instance
            TIMESTAMPS.put(service+"@"+instance.toUrl(), now);
        }
        return now;
    }

    @Override
    public Long version(String service) {
        return VERSIONS.get(service);
    }

    @Override
    public Map<String, Long> versions(String... services) {
        return  Arrays.stream(services)
                .collect(Collectors.toMap(x->x, VERSIONS::get, (a, b)->b));
    }

    /**
     * get current server info snapshot
     * @return
     */
    public static synchronized Snapshot snapshot() {
        LinkedMultiValueMap<String, InstanceMeta> registry = new LinkedMultiValueMap<>();
        registry.addAll(REGISTRY);
        Map<String, Long> versions = new HashMap<>(VERSIONS);
        Map<String, Long> timestamps = new HashMap<>(TIMESTAMPS);
        return new Snapshot(registry, versions, timestamps, VERSION.get());
    }

    //TODO 因为其为静态方法，可将其提取成工具类方法
    public static synchronized long restore(Snapshot snapshot) {
        REGISTRY.clear();
        VERSIONS.clear();
        TIMESTAMPS.clear();

        REGISTRY.addAll(snapshot.getREGISTRY());
        VERSIONS.putAll(snapshot.getVERSIONS());
        TIMESTAMPS.putAll(snapshot.getTIMESTAMPS());
        VERSION.set(snapshot.getVersion());

        return snapshot.getVersion();
    }
}
