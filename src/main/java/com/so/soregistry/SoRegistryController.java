package com.so.soregistry;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.so.soregistry.cluster.Cluster;
import com.so.soregistry.cluster.Server;
import com.so.soregistry.cluster.Snapshot;
import com.so.soregistry.model.InstanceMeta;
import com.so.soregistry.service.RegistryService;
import com.so.soregistry.service.SoRegistryService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-04-19
 */
@RestController
@Slf4j
public class SoRegistryController {

    @Autowired
    RegistryService registryService;

    @Autowired
    Cluster cluster;

    @RequestMapping("/reg")
    public InstanceMeta register(@RequestParam String service, @RequestBody InstanceMeta instance)
    {
        log.info(" ===> register {} @ {}", service, instance);
        //only leader allow option
        checkLeader();
        return registryService.register(service, instance);
    }

    private void checkLeader() {
        if(!cluster.self().isLeader()) {
            throw new RuntimeException("current server is not a leader, the leader is " + cluster.leader().getUrl());
        }
    }

    @RequestMapping("/unreg")
    public InstanceMeta unregister(@RequestParam String service, @RequestBody InstanceMeta instance)
    {
        log.info(" ===> unregister {} @ {}", service, instance);
        //only leader allow option
        checkLeader();
        return registryService.unregister(service, instance);
    }

    @RequestMapping("/findAll")
    public List<InstanceMeta> findAllInstances(@RequestParam String service)
    {
        log.info(" ===> findAllInstances {}", service);
        return registryService.getAllInstances(service);
    }

    @RequestMapping("/renew")
    public long renew(@RequestParam String service, @RequestBody InstanceMeta instance)
    {
        log.info(" ===> renew {} @ {}", service, instance);
        //only leader allow option
        checkLeader();
        return registryService.renew(instance, service);
    }

    @RequestMapping("/version")
    public long version(@RequestParam String service)
    {
        log.info(" ===> version {}", service);
        //only leader allow option
        checkLeader();
        return registryService.version(service);
    }

    @RequestMapping("/versions")
    public Map<String, Long> versions(@RequestParam String services)
    {
        log.info(" ===> versions {}", services);
        return registryService.versions(services.split(","));
    }

    /**
     * 获取本实例集群信息，用于探活
     * @return
     */
    @RequestMapping("/info")
    public Server info()
    {
        log.info(" ===> self info: {}", cluster.self());
        return cluster.self();
    }

    /**
     * 获取所有实例信息(集群信息)
     * @return
     */
    @RequestMapping("/cluster")
    public List<Server> cluster()
    {
        log.info(" ===> cluster: {}", cluster.getServers());
        return cluster.getServers();
    }

    /**
     * 获取leader信息
     * @return
     */
    @RequestMapping("/leader")
    public Server leader()
    {
        log.info(" ===> leader: {}", cluster.leader());
        return cluster.leader();
    }

    @RequestMapping("/setSelfLeader")
    public Server setSelfLeader()
    {
        cluster.self().setLeader(true);
        log.info(" ===> setSelfLeader: {}", cluster.self());
        return cluster.self();
    }

    @RequestMapping("/snapshot")
    public Snapshot snapshot() {
        return SoRegistryService.snapshot();
    }
}
