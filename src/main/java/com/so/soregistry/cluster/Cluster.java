package com.so.soregistry.cluster;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;

import com.so.soregistry.SoRegistryConfigProperties;
import com.so.soregistry.service.SoRegistryService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 定义注册中心集群(实例的抽象概念)
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-04-22
 */
@Slf4j
public class Cluster {

    @Value("${server.port}")
    String port;

    String host;

    Server MYSELF;

    SoRegistryConfigProperties registryConfigProperties;

    @Getter
    private List<Server> servers;

    public Cluster(SoRegistryConfigProperties registryConfigProperties) {
        this.registryConfigProperties = registryConfigProperties;
    }

    /**
     * 1. parse host and convert url, init servers \n
     * 2. schedule heartbeat
     *   - update servers
     *   - elect leader
     */
    public void init() {
//        self();
        try {
            this.host = new InetUtils(new InetUtilsProperties())
                    .findFirstNonLoopbackHostInfo().getIpAddress(); //get first none localhost info
            log.info("cluster init, get current server first non loopback  host:{}", host);
        } catch (Exception e) {
            host = "127.0.0.1";
            log.error("cluster init error", e);
        }
        String scheme = "http://";
        //MYSELF default: status-true, leader-flase, version-0
        MYSELF = new Server(scheme + host + ":" + port, true, false, -1L);
        log.info("cluster init, myself:{}", MYSELF);

        //extra init servers method
        initServers();
        //check server health
        new ServerHealth(this).checkServerHealth();
    }

    private void initServers() {
        //get serverList
        List<Server> servers = new ArrayList<>();
        registryConfigProperties.getServerList().forEach(url -> {
            //handle host
            if (url.contains("localhost")) {
                url = url.replace("localhost", host);
            } else if(url.contains("127.0.0.1")) {
                url = url.replace("127.0.0.1", host);
            }
            if (url.equals(MYSELF.getUrl())) {
                log.debug("cluster init, add self, server:{}", MYSELF);
                servers.add(MYSELF);
            } else {
                Server server = new Server(url, false, false, -1L);
                log.debug("cluster init, not self, server:{}", server);
                servers.add(server);
            }
        });
        this.servers = servers;
    }

    public Server self() {
        //???
//        if (MYSELF == null) {
//            MYSELF = new Server(host + ":" + port, true, false, -1L);
//            MYSELF.setVersion(SoRegistryService.VERSION.get());
//        }
        //MYSELF always not null?
        MYSELF.setVersion(SoRegistryService.VERSION.get());
        return MYSELF;
    }
    /**
     *
     */

    public Server leader() {
       return this.servers.stream().filter(Server::isLeader)
                .findFirst().orElse(null);
    }

}
