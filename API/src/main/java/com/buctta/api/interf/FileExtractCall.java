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
@RequestMapping("/api/fileextract")

public class FileExtractCall {
    private static final String UPLOAD_DIR = "/fileextract/temp/";

    @PostMapping("/temp")
    public CallBackContainer<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new CallBackContainer<>("400", "文件为空", "");
        }

        String original = file.getOriginalFilename();
        if (original == null ||
                !(original.toLowerCase().endsWith(".docx") || original.toLowerCase().endsWith(".pdf"))) {
            return new CallBackContainer<>("400", "仅支持 .docx / .pdf", "");
        }

        try {
            // 1. 保存到本地
            Path dir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            String uuid = UUID.randomUUID().toString();
            String ext = original.substring(original.lastIndexOf('.'));
            Path localPath = dir.resolve(uuid + ext);
            file.transferTo(localPath.toFile());

            // 2. 提取文本
            String content = FileContentExtractor.extract(localPath.toFile());

            // 3. 可选：立即删除临时文件
            Files.deleteIfExists(localPath);

            return new CallBackContainer<>("200", "解析成功", content);
        } catch (IOException e) {
            return new CallBackContainer<>("500", "解析失败：" + e.getMessage(), "");
        }
    }
}