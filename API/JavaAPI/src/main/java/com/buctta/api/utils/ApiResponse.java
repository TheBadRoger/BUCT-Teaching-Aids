package com.buctta.api.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String msg;
    private long timestamp;
    private T data;

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(BusinessStatus.SUCCESS.getCode(), BusinessStatus.SUCCESS.getTemplate(), System.currentTimeMillis(), null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(BusinessStatus.SUCCESS.getCode(), BusinessStatus.SUCCESS.getTemplate(), System.currentTimeMillis(), data);
    }

    public static <T> ApiResponse<T> fail(int code, String msg) {
        return new ApiResponse<T>(code, msg, System.currentTimeMillis(), null);
    }

    public static <T> ApiResponse<T> fail(BusinessStatus status) {
        return new ApiResponse<T>(status.getCode(), status.getTemplate(), System.currentTimeMillis(), null);
    }

    public static <T> ApiResponse<T> fail(BusinessStatus status, Object... args) {
        return new ApiResponse<>(status.getCode(), status.format(args), System.currentTimeMillis(), null);
    }
}