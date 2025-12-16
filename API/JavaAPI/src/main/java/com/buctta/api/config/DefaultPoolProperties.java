package com.buctta.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "defaultpool")
public class DefaultPoolProperties {
    private int coreSize;
    private int maxSize;
    private int queueCapacity;
    private String threadNamePrefix;
    private int keepAliveSeconds;
}