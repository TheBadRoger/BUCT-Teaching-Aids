package com.buctta.api.controller;

import com.buctta.api.utils.TextUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/generate")
public class FileDownloadCtrl {
    // 使用项目运行目录下的 temp 文件夹
    @Value("${file.download-dir:./temp/}")
    private String STATIC_DIR;

    @GetMapping("/download/{timeStamp}")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable String timeStamp,
            @RequestParam String text) throws IOException {

        String fileName = "judgereport_" + timeStamp + ".xlsx";

        // 1. 生成 Excel 到内存（不落地磁盘）
        byte[] excelBytes = generateExcelBytes(text);

        // 2. 构造下载头
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");  // 处理空格编码问题

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=utf-8''" + encodedFileName);

        // 3. 返回 ResponseEntity
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }


    // 生成 Excel 字节数组（内存操作，不写磁盘）

    private byte[] generateExcelBytes(String text) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("评判报告");
            insertRow(sheet, 0, new String[]{"姓名", "学号", "班级", "日期", "报告名称", "分数", "评判依据"});

            String[] parts = TextUtils.spiltByLine(text, "^========== 第 \\d+/\\d+ 个文件 ==========\\R");
            for (int i = 0; i < parts.length; i++) {
                String[] rowData = extractRowData(parts[i]);
                if (rowData.length > 0) {
                    insertRow(sheet, i + 1, rowData);
                }
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    //从单个文件的批改结果中提取行数据
    private String[] extractRowData(String text) {
        String[] fields = {"姓名", "学号", "班级", "日期", "报告名称", "分数", "评判依据"};
        String[] rowData = new String[7];

        for (int i = 0; i < fields.length; i++) {
            // 查找 "字段名：值" 的格式
            String pattern = "^" + fields[i] + "：(.+)$";
            String[] values = TextUtils.ExtractInfo(text, pattern);
            rowData[i] = (values.length > 0) ? values[0] : "";
        }

        return rowData;
    }

    private void insertRow(Sheet sheet, int rowNum, String[] values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
            sheet.autoSizeColumn(i);
        }
    }
}
