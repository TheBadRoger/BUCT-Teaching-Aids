package com.buctta.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 临时文件清理服务
 * 定期清理 temp 目录下的过期文件
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class TempFileCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(TempFileCleanupService.class);

    // 临时文件存储目录
    private static final String TEMP_DIR = "src/main/resources/static/temp/";

    // 文件保留时长（小时），默认24小时
    private static final int FILE_RETENTION_HOURS = 24;

    /**
     * 定时任务：每小时执行一次清理
     * cron 表达式: 0 0 * * * ? 表示每小时的0分0秒执行
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredFiles() {
        logger.info("开始清理过期的临时文件...");

        try {
            File tempDir = new File(TEMP_DIR);
            if (!tempDir.exists()) {
                logger.warn("临时文件目录不存在: {}", TEMP_DIR);
                return;
            }

            File[] files = tempDir.listFiles();
            if (files == null || files.length == 0) {
                logger.info("临时文件目录为空");
                return;
            }

            Instant cutoffTime = Instant.now().minus(FILE_RETENTION_HOURS, ChronoUnit.HOURS);
            int deletedCount = 0;

            for (File file : files) {
                if (file.isFile()) {
                    try {
                        Path path = Paths.get(file.getAbsolutePath());
                        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                        Instant lastModified = attrs.lastModifiedTime().toInstant();

                        // 如果文件的最后修改时间早于截断时间，则删除
                        if (lastModified.isBefore(cutoffTime)) {
                            boolean deleted = file.delete();
                            if (deleted) {
                                logger.info("已删除过期文件: {}", file.getName());
                                deletedCount++;
                            } else {
                                logger.warn("删除失败: {}", file.getName());
                            }
                        }
                    } catch (Exception e) {
                        logger.error("处理文件时出错: {}", file.getName(), e);
                    }
                }
            }

            logger.info("清理完成，共删除 {} 个文件", deletedCount);

        } catch (Exception e) {
            logger.error("清理临时文件时出错", e);
        }
    }
}

