package com.so.soregistry.elect;

import java.util.List;

import com.so.soregistry.cluster.Server;

/**
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-04-22
 */
public interface ElectLeader {
    void elect(List<Server> servers);
}
