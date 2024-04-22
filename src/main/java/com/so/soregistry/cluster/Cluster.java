package com.so.soregistry.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;

import com.so.soregistry.SoRegistryConfigProperties;

import http.HttpInvoker;
import http.OkHttpInvoker;
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

    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    long timeout = 5_000;

    public Cluster(SoRegistryConfigProperties registryConfigProperties) {
        this.registryConfigProperties = registryConfigProperties;
    }

    /**
     * 1. parse host and convert url, init servers
     * 2. schedule heartbeat
     *   - update servers
     *   - elect leader
     */
    public void init() {
        try {
            this.host = new InetUtils(new InetUtilsProperties())
                    .findFirstNonLoopbackHostInfo().getIpAddress(); //get first none localhost info
            log.info("cluster init, get current server host:{}", host);
        } catch (Exception e) {
            host = "127.0.0.1";
            log.error("cluster init error", e);
        }
        String scheme = "http://";
        //MYSELF default: status-true, leader-flase, version-0
        MYSELF = new Server(scheme + host + ":" + port, true, false, -1L);
        log.info("cluster init, myself:{}", MYSELF);

        //get serverList
        List<Server> serverList = new ArrayList<>();
        registryConfigProperties.getServerList().forEach(url -> {
            //handle host
            if (url.contains("localhost")) {
                url = url.replace("localhost", host);
            } else if(url.contains("127.0.0.1")) {
                url = url.replace("127.0.0.1", host);
            }
            if (url.equals(MYSELF.getUrl())) {
                serverList.add(MYSELF);
            } else {
                Server server = new Server(url, false, false, -1L);
                log.info("cluster init, not self, server:{}", server);
                serverList.add(server);
            }
        });
        this.servers = serverList;

        //keep alive: every 5s check
        executor.scheduleWithFixedDelay(() -> {
            try {
                log.info("schedule start========");
                updateServers();
                electLeader();
                log.info("schedule end========");
            }catch (Exception e) {
                log.error("cluster heartbeat error", e);
                e.printStackTrace();
            }
        }, 0, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     *
     */
    private void electLeader() {
        log.info("electLeader start========");
        //filter leaders
        List<Server> leaders = this.servers.stream().filter(Server::isStatus)
                .filter(Server::isLeader).toList();
        if (leaders.isEmpty()) {
            log.info(" ===>>>  elect for no leader: {}" ,servers);
            elect();
        } else if (leaders.size() > 1) {
            log.debug("===>>> elect for more than one leader: {}" , servers);
            elect();
        } else {
            log.info(" ===>>> no need election for leader: {}", leaders.get(0));
        }
        log.info("electLeader end========");
    }

    private void elect() {
        //define candidate
        Server candidate = null;
        for (Server server : servers) {
            if (server.isStatus()) {
                if (candidate == null) {
                    candidate = server;
                } else {
                    if (candidate.hashCode() > server.hashCode()) {
                        candidate = server;
                    }
                }
            }
        }
        if (candidate != null) {
            candidate.setLeader(true);
            log.info(" ===> elect for leader:{}", candidate);
        } else {
            log.info(" ===>>> elect failed for no leaders: {}" ,servers);
        }
    }

    private static final String SERVER_INFO_URL = "/info";

    /**
     * 1. invoke url/info to make sure server is alive
     * 2. if alive -> status: true
     * 3. if not alive -> status: false
     */
    private void updateServers() {
        log.info("updateServers start========");
        //self??
        servers.forEach(server -> {
            try {
                Server serverInfo = HttpInvoker.httpGet(server.getUrl() + SERVER_INFO_URL, Server.class);
                log.info("cluster update, health check ok, server:{}", serverInfo);
                if (serverInfo != null) {
                    server.setStatus(true); //its alive
                    server.setLeader(serverInfo.isLeader());
                    server.setVersion(serverInfo.getVersion());
                }
            } catch (Exception e) {
                log.error("cluster update error, health check fail, ", e);
                server.setStatus(false);
                server.setLeader(false);
            }
        });
        log.info("updateServers end========");
    }

    public Server self() {
        return MYSELF;
    }

    public Server leader() {
       return this.servers.stream().filter(Server::isLeader)
                .findFirst().orElse(null);
    }

}
