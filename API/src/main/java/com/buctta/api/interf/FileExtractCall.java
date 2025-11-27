package com.buctta.api.interf;

import com.buctta.api.service.FileExtract;
import com.buctta.api.utils.CallBackContainer;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;

@RestController
@RequestMapping("/api/fileextract")

public class FileExtractCall {
    @Resource
    private FileExtract fileExtract;

    @PostMapping("/temp")
    public CallBackContainer<String> FileUploader(@RequestParam("files") MultipartFile file) throws IOException {
        try{
            byte[] bytes = file.getBytes();
            String uploadDir = "/fileextract/temp";
            File uploadedFile = new File(uploadDir + file.getOriginalFilename());
            file.transferTo(uploadedFile);
            return fileExtract.DocumentExtractor(uploadedFile.getPath());
        }
        catch (IOException e) {
            return new CallBackContainer<>("-102", "解析失败", "");
        }
    }
}