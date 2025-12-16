package com.buctta.api.utils;

import com.buctta.api.config.DefaultPoolProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

public final class ThreadPool {
    public static final ThreadPoolTaskExecutor INSTANCE = defaultProp();
    /**
     * 快速创建 ThreadPoolTaskExecutor
     * 调用方负责 shutdown
     */
    public static ThreadPoolTaskExecutor create(int corePoolSize,
                                                int maxPoolSize,
                                                int queueCapacity,
                                                String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        // 可选策略 & 优雅关机
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize(); // 关键
        return executor;
    }

    /* 重载：使用默认前缀 */
    public static ThreadPoolTaskExecutor create(int core, int max, int queue) {
        return create(core, max, queue, "custom-thread-");
    }

    public  static ThreadPoolTaskExecutor defaultProp() {
        DefaultPoolProperties prop = new DefaultPoolProperties();
        return create(prop.getCoreSize(), prop.getMaxSize(),prop.getQueueCapacity(), prop.getThreadNamePrefix());
    }
}