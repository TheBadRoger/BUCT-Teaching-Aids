package com.buctta.api.utils;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseContainer<T> {
    private String code;
    private String msg;
    private T data;

    public static ResponseContainer Failed(String code, String msg) {
        return new ResponseContainer(code,msg,null);
    }
}
