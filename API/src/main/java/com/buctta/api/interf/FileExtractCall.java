package com.buctta.api.interf;

import com.buctta.api.service.FileExtract;
import com.buctta.api.utils.CallBackContainer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/fileextract")

public class FileExtractCall {
    @Resource
    private FileExtract fileExtract;

    @PostMapping("/docx")
    public CallBackContainer<String> ExtractDocx(@RequestBody String path){
        System.out.println("收到文件解析请求："+path+"\n");
        try{
            return new CallBackContainer<>("0","解析成功",fileExtract.ExtractDocxFile(path));
        }
        catch (IOException e){
            System.out.println("解析失败。异常为："+e);
            return new CallBackContainer<String>("-100","解析失败。异常为："+e,"");
        }
    }

    @PostMapping("/pdf")
    public CallBackContainer<String> ExtractPdf(@RequestBody String path){
        System.out.println("收到文件解析请求："+path+"\n");
        try{
            return new CallBackContainer<>("0","解析成功",fileExtract.ExtractPdfFile(path));
        }
        catch (IOException e){
            System.out.println("解析失败。异常为："+e);
            return new CallBackContainer<String>("-100","解析失败。异常为："+e,"");
        }
    }
}
