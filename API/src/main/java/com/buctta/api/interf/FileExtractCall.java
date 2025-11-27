package com.buctta.api.interf;

import com.buctta.api.utils.FileContentExtractor;
import com.buctta.api.utils.CallBackContainer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
public class FileExtractCall {

    @PostMapping("/api/fileextract/temp")
    public CallBackContainer<String> upload(@RequestParam("files") MultipartFile file) {
        /* 1. 基础校验 */
        if (file.isEmpty()) {
            return new CallBackContainer<>("400", "文件为空", "");
        }
        String original = file.getOriginalFilename();
        if (original == null ||
                !(original.toLowerCase().endsWith(".docx") || original.toLowerCase().endsWith(".pdf"))) {
            return new CallBackContainer<>("400", "仅支持 .docx / .pdf", "");
        }

        try {
            /* 2. 直接从内存流解析 */
            String content;
            String ext = original.substring(original.lastIndexOf('.'));
            if (ext.equalsIgnoreCase(".docx")) {
                content = FileContentExtractor.parseDocxByIS(file.getInputStream());
            } else {   // pdf
                content = FileContentExtractor.parsePdfByIS(file.getInputStream());
            }

            return new CallBackContainer<>("200", "解析成功", content);
        } catch (Exception e) {
            return new CallBackContainer<>("500", "解析失败：" + e.getMessage(), "");
        }
    }
}