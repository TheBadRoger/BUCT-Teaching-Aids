package com.buctta.api.utils;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileContentExtractor {
    public static String extract(File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".docx")) {
            return extractDocx(file);
        }
        else if (name.endsWith(".pdf")) {
            return extractPdf(file);
        }
        else {
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
        try (PDDocument doc = Loader.loadPDF(file)) {  // ← 使用 Loader
            return new PDFTextStripper().getText(doc);
        }
    }

    public static String parseDocxByIS(InputStream in) throws Exception {
        try (var doc = new XWPFDocument(in);
             var extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    public static String parsePdfByIS(InputStream in) throws IOException {
        try (PDDocument document = Loader.loadPDF(in.readAllBytes())) {  // ← 使用 Loader
            var stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}