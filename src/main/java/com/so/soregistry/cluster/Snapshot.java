package com.so.soregistry.cluster;

import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;

import com.so.soregistry.model.InstanceMeta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * need sync cluster server info
 * @author someecho <linghan.ma@gmail.com>
 * Created on 2024-05-06
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Snapshot {
    LinkedMultiValueMap<String, InstanceMeta> REGISTRY; //it must be LinkedMultiValueMap because it has to be stored
    Map<String, Long> VERSIONS;
    Map<String, Long> TIMESTAMPS;
    long version;
}
