package com.so.soregistry.service;

import java.util.List;
import java.util.Map;

import com.so.soregistry.model.InstanceMeta;

/**
 * 注册服务接口
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-04-19
 */
public interface RegistryService {
    //base method

    /**
     * 注册服务实例到服务目录
     * @param service
     * @param instance
     * @return
     */
    InstanceMeta register(String service, InstanceMeta instance);
    /**
     * 注销服务实例
     * @param service
     * @param instance
     * @return
     */
    InstanceMeta unregister(String service, InstanceMeta instance);
    /**
     * 获取服务实例列表
     * @param service
     * @return
     */
    List<InstanceMeta> getAllInstances(String service);

    // todo add some advanced feature
    /**
     * 续约服务实例
     * @param instance
     * @param services
     * @return
     */
    long renew(InstanceMeta instance, String... services);

    /**
     * 获取服务版本
     * @param service
     * @return
     */
    Long version(String service);
    /**
     * 批量获取服务版本
     * @param services service info
     * @return
     */
    Map<String, Long> versions(String... services);

    /**
     *
     * @return
     */
    Map<String, Long> getTimestamps();
}
