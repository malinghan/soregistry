package com.so.soregistry.cluster;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-04-24
 */
@Slf4j
public class Election {

    public void electLeader(List<Server> servers) {
        log.info("electLeader start========");
        //filter leaders
        List<Server> leaders = servers.stream().filter(Server::isStatus)
                .filter(Server::isLeader).toList();
        if (leaders.isEmpty()) {
            log.warn(" ===>>>  elect for no leader: {}" ,servers);
            elect(servers);
        } else if (leaders.size() > 1) {
            log.warn("===>>> elect for more than one leader: {}" , servers);
            elect(servers);
        } else {
            log.debug(" ===>>> no need election for leader: {}", leaders.get(0));
        }
        log.info("electLeader end========");
    }

    /**
     * 真正的选主算法所在，可以抽象成不同的算法实现
     * 1. 选主算法1：外部定义一个分布式锁，谁拿到锁，谁是主
     * 2. 选主算法2: 分布式一致性算法,如paxos、raft，很复杂
     * @param servers
     */
    private void elect(List<Server> servers) {
        //define candidate
        Server candidate = null;
        for (Server server : servers) {
            //to make sure leader is only one
            server.setLeader(false);
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
}
