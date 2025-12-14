package com.buctta.api.utils;

public class CallBackContainer<T> {
    private String code;
    private String msg;
    private T data;

    public CallBackContainer() {}
    public CallBackContainer(T data) { this.data = data; }
    public CallBackContainer(String code, String msg, T data) {
        this.code = code; this.msg = msg; this.data = data;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public static CallBackContainer Succeed(){
        return new CallBackContainer("0","Succeed",null);
    }

    public static <T> CallBackContainer<T> Succeed(T data){
        return new CallBackContainer<T>("0","Succeed",data);
    }

    public static <T> CallBackContainer<T>Succeed(T data, String msg){
        return new CallBackContainer<T>("0",msg, data);
    }

    public static CallBackContainer Failed(String code,String msg){
        return new CallBackContainer(code,msg,null);
    }
}
