package com.buctta.api.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseContainer<T> {
    private Integer code;
    private String msg;
    private T data;
}

