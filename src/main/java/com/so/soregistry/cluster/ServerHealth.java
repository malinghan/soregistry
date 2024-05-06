package com.so.soregistry.cluster;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.so.soregistry.service.SoRegistryService;

import http.HttpInvoker;
import lombok.extern.slf4j.Slf4j;

/**
 * check health for servers
 * 1. updateServers()
 * 2. doElect();
 * 3. syncSnapshotFromLeader();
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-04-24
 */
@Slf4j
public class ServerHealth {

    //copy from Cluster
    final Cluster cluster;

    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    //adjust timeout from 5s to 1s
//    long interval = 1_000;
    long interval = 10_000;


    public ServerHealth(Cluster cluster) {
        this.cluster = cluster;
    }

    public void checkServerHealth() {
        //keep alive: every 5s check
        executor.scheduleWithFixedDelay(() -> {
            try {
                log.info("schedule start========");
                updateServers(); //step1, updateServers
                electLeader(); // step2, elect leaders
                syncSnapshotFromLeader(); // step3, sync snapshot from leader
                log.info("schedule end========");
            }catch (Exception e) {
                log.error("cluster heartbeat error", e);
                e.printStackTrace();
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    private void syncSnapshotFromLeader() {
        //get self and leader
        Server self = cluster.self();
        Server leader = cluster.leader();
        log.debug(" ===> leader version: {}, myversion: {}", leader.getVersion(), self.getVersion());
        if (!self.isLeader() && self.getVersion() < leader.getVersion()) {
            log.debug(" ===> sync snapshot from leader:{}", leader);
            //todo 将 /snapshot改成可配置
            Snapshot snapshot =  HttpInvoker.httpGet(leader.getUrl() + "/snapshot", Snapshot.class);
            log.debug(" ===> sync and restore snapshot: {}",  snapshot);
            SoRegistryService.restore(snapshot);
        }
        //what if self.getVersion() > leader.getVersion() ??
    }

    private void electLeader() {
        new Election().electLeader(cluster.getServers());
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
        List<Server> servers = cluster.getServers();
        //parallel
        servers.stream().parallel().forEach(server -> {
            try {
                //skip myself
                if (server.equals(cluster.self())) return;  // 如果是自己，则不去更新状态
                Server serverInfo = HttpInvoker.httpGet(server.getUrl() + SERVER_INFO_URL, Server.class);
                log.debug("cluster update, health check ok, server:{}", serverInfo);
                if (serverInfo != null) {
                    server.setStatus(true); //its alive
                    server.setLeader(serverInfo.isLeader());
                    server.setVersion(serverInfo.getVersion());
                } else {
                    log.debug(" ===>>> health check failed serverInfo is null ");
                    server.setStatus(false);
                    server.setLeader(false);
                }
            } catch (Exception e) {
                log.error("cluster update error, health check fail, ", e);
                server.setStatus(false);
                server.setLeader(false);
            }
        });
        log.info("updateServers end========");
    }


}
