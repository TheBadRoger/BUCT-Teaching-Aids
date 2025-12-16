package com.buctta.api.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aijudge") // 前缀=ai
public class AIJudgementProperties {
    private String endpoint;
    private String authKey;
    private String stopMark;
}
