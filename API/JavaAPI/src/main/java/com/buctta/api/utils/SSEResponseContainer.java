package com.buctta.api.utils;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class SSEResponseContainer<T> {
    private String type;   // fileStart / message / fileEnd / done / error
    private T data;
}