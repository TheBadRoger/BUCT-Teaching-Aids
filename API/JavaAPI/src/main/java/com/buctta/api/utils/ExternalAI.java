package com.buctta.api.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalAI {
    private String EndPoint;
    private String AuthKey;
    private String StopMark;
}
