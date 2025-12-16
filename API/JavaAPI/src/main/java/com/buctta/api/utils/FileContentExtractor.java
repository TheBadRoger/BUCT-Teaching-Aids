package com.buctta.api.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileContentExtractor {

    /**
     * 根据后缀自动选择解析器，返回纯文本
     */
    public static String extract(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".docx")) {
            return extractDocx(file);
        } else if (name.endsWith(".pdf")) {
            return extractPdf(file);
        } else {
            throw new IOException("不支持的文件类型");
        }
    }

    private static String extractDocx(File file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private static String extractPdf(File file) throws IOException {
        try (PDDocument doc = PDDocument.load(file)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    /* ---------------- 工具方法 ---------------- */
    public static String parseDocxByIS(InputStream in) throws Exception {
        // 需要 poi-ooxml 依赖
        try (var doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(in)) {
            var extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc);
            return extractor.getText();
        }
    }

    public static String parsePdfByIS(InputStream in) throws Exception {
        // 需要 pdfbox 依赖
        var document = org.apache.pdfbox.pdmodel.PDDocument.load(in);
        var stripper = new org.apache.pdfbox.text.PDFTextStripper();
        return stripper.getText(document);
    }
}