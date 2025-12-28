package com.buctta.api.controller;

import com.buctta.api.utils.TextUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/generate")
public class FileDownloadCtrl {
    // 使用项目运行目录下的 temp 文件夹
    @Value("${file.download-dir:./temp/}")
    private String STATIC_DIR;

    @GetMapping("/judgereport/{timeStamp}")
    public void downloadReport(HttpServletResponse response, @PathVariable String timeStamp, @RequestParam String text) throws IOException {

        String fileName = "judgereport_" + timeStamp + ".xlsx";

        Path dirPath  = Paths.get(STATIC_DIR).toAbsolutePath().normalize();
        Files.createDirectories(dirPath);
        Path filePath = dirPath.resolve(fileName);

        // 2. 生成Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet newsheet = workbook.createSheet("评判报告");

        insertRow(newsheet, 0, new String[]{"姓名", "学号", "班级", "日期", "报告名称", "分数", "评判依据"});

        // 3. 解析文本数据
        String[] parts = TextUtils.spiltByLine(text, "^========== 第 \\d+/\\d+ 个文件 ==========\\R");
        for (int i = 0; i < parts.length; i++) {
            String[] rowData = extractRowData(parts[i]);
            if (rowData.length > 0) {
                insertRow(newsheet, i + 1, rowData);
            }
        }

        // 4. 保存文件到静态目录
        try (OutputStream fos = Files.newOutputStream(filePath)) {
            workbook.write(fos);
        } finally {
            workbook.close();
        }

        // 5. 返回下载链接（JSON格式）
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=utf-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        Files.copy(filePath, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 从单个文件的批改结果中提取行数据
     */
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
