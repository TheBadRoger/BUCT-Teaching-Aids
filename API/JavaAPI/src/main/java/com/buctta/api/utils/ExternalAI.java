package com.buctta.api.utils;

import com.buctta.api.config.AIJudgementProperties;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalAI {
    private String EndPoint;
    private String AuthKey;
    private String StopMark;

    public static ExternalAI AIJudgement(){
        AIJudgementProperties props = new AIJudgementProperties();
        return new ExternalAI(props.getEndpoint(), props.getAuthKey(), props.getStopMark());
    }
}
