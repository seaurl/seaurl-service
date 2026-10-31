package com.seaurl.platform.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务线程池配置。
 *
 * 注意：业务里绝不要用 CompletableFuture.runAsync(task) 无参版本，它复用 JVM 级的
 * ForkJoinPool.commonPool()。commonPool 的 worker 线程在首次被使用时捕获那一刻的
 * 线程上下文类加载器（TCCL），该时机在不同环境（本地 vs 容器）下不一致，可能导致
 * Jakarta Mail 的 ServiceLoader 找不到 activation 的 SPI 实现
 * （Provider for jakarta.activation.spi.MailcapRegistryProvider cannot be found）。
 *
 * 这里定义的专用线程池由 Spring 在启动阶段创建，worker 线程可靠地持有应用类加载器，
 * 且专用于阻塞式邮件同步 IO，避免和 commonPool 互相拖累。
 */
@Configuration
public class AsyncConfig {

    @Bean("mailSyncExecutor")
    public TaskExecutor mailSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-sync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
