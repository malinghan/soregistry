package com.so.soregistry;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * auto configuration properties for so registry
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-04-22
 */
@Data
@ConfigurationProperties(prefix = "soregistry")
public class SoRegistryConfigProperties {
    private List<String> serverList;
}
