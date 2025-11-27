package com.buctta.api.serviceimp;

import com.buctta.api.service.FileExtract;
import com.buctta.api.utils.CallBackContainer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class IMPL_FileExtract implements FileExtract {

    @Override
    public String ExtractDocxFile(String path) throws IOException {
        String content="";
        try (FileInputStream fis = new FileInputStream(path);XWPFDocument document = new XWPFDocument(fis)) {
            for(XWPFParagraph p : document.getParagraphs()) {
                content.concat(p.getText());
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        System.out.print(cell.getText() + "\t");
                    }
                    System.out.println();
                }
            }
        }
        return content;
    }

    @Override
    public String ExtractPdfFile(String path) throws IOException {
        String content="";
        try(FileInputStream fis = new FileInputStream(path);PDDocument document = PDDocument.load(fis)) {
            PDFTextStripper catchText = new PDFTextStripper();
            content=catchText.getText(document);
        }
        return content;
    }

    @Override
    public CallBackContainer<String> DocumentExtractor(String path) throws IOException {
        String ext = path.substring(path.lastIndexOf(".") + 1);
        if(ext.equals("docx")){
            return new CallBackContainer<>("1","解析成功",ExtractDocxFile(path));
        }
        else if(ext.equals("pdf")){
            return new CallBackContainer<>("1","解析成功",ExtractPdfFile(path));
        }
        else return new CallBackContainer<>("-101","无法解析文件类型","");
    }
}
