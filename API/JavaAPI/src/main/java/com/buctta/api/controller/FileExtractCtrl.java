package com.buctta.api.controller;

import com.buctta.api.utils.ApiResponse;
import com.buctta.api.utils.BusinessStatus;
import com.buctta.api.utils.FileContentExtractor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/fileextract")
public class FileExtractCtrl {

    @PostMapping("/temp")
    public ApiResponse<List<DocResult>> upload(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ApiResponse.fail(BusinessStatus.RESOURCE_NOT_FOUND);
        }

        List<DocResult> list = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            if (file.isEmpty()) {          // 跳过空文件
                continue;
            }
            String original = file.getOriginalFilename();
            if (original == null ||
                    !(original.toLowerCase().endsWith(".docx") || original.toLowerCase().endsWith(".pdf"))) {
                // 单文件格式错误，记一条失败记录，继续
                list.add(new DocResult(original, false, "仅支持 .docx / .pdf", null));
                continue;
            }
            try {
                String ext = original.substring(original.lastIndexOf('.'));
                String content = ext.equalsIgnoreCase(".docx")
                        ? FileContentExtractor.parseDocxByIS(file.getInputStream())
                        : FileContentExtractor.parsePdfByIS(file.getInputStream());
                list.add(new DocResult(original, true, "解析成功", content));
            } catch (Exception e) {
                // 单文件解析异常，记一条失败记录，继续
                list.add(new DocResult(original, false, "解析失败：" + e.getMessage(), null));
            }
        }

        // 全部跑完再统一返回
        return ApiResponse.ok(list);
    }

    /* 简单 DTO，用来装单个文件的结果 */
    public static class DocResult {
        public String fileName;
        public boolean success;
        public String msg;
        public String content;

        public DocResult(String fileName, boolean success, String msg, String content) {
            this.fileName = fileName;
            this.success = success;
            this.msg = msg;
            this.content = content;
        }
    }
}