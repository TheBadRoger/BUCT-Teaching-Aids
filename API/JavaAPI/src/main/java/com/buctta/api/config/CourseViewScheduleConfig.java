package com.buctta.api.config;

import com.buctta.api.service.CourseViewService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 课程访问量定时任务配置
 * 定期将Redis中的访问数据同步到MySQL数据库
 */
@Slf4j
@Configuration
@EnableScheduling
public class CourseViewScheduleConfig {

    @Resource
    private CourseViewService courseViewService;

    /**
     * 每小时执行一次，将Redis数据同步到MySQL
     * 使用cron表达式：0 0 * * * * 表示每小时的整点
     */
    @Scheduled(cron = "0 0 * * * *")
    public void syncViewCountsHourly() {
        log.info("Executing hourly sync task...");
        try {
            courseViewService.syncViewCountsToDatabase();
        }
        catch (Exception e) {
            log.error("Error in hourly sync task", e);
        }
    }

    /**
     * 每天凌晨2点执行一次缓存重建
     * 确保Redis和数据库之间的数据一致性
     * cron表达式：0 0 2 * * * 表示每天2:00:00
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void rebuildCacheDailyAtMidnight() {
        log.info("Executing daily cache rebuild task...");
        try {
            courseViewService.rebuildCourseViewCache();
        }
        catch (Exception e) {
            log.error("Error in daily cache rebuild task", e);
        }
    }

    /**
     * 每5分钟执行一次增量同步（可选）
     * 用于更加频繁的数据持久化
     * cron表达式：0
     *
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void syncViewCountsEveryHalfHour() {
        log.debug("Executing half-hourly incremental sync task...");
        try {
            courseViewService.syncViewCountsToDatabase();
        }
        catch (Exception e) {
            log.error("Error in half-hourly sync task", e);
        }
    }
}

