package com.buctta.api.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class SSEResponseContainer<T> {
    private String type;
    private T data;
}