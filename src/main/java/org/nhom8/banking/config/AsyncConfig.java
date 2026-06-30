package org.nhom8.banking.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nhom8.banking.repository.TransferSessionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
@RequiredArgsConstructor
public class AsyncConfig {

    private final TransferSessionRepository transferSessionRepository;

    /** Xóa phiên hết hạn mỗi 10 phút để tránh bảng tăng vô tận */
    @Scheduled(fixedDelay = 600_000)
    public void cleanExpiredSessions() {
        transferSessionRepository.deleteExpired(LocalDateTime.now());
        log.debug("Expired transfer sessions cleaned");
    }

    /**
     * Thread pool riêng để gửi email bất đồng bộ.
     * Tránh block request thread khi SMTP bị chậm.
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool xử lý giả lập liên ngân hàng bất đồng bộ.
     * awaitTermination=15s: đủ để các giao dịch đang chạy hoàn tất khi shutdown.
     */
    @Bean(name = "transferExecutor")
    public Executor transferExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("transfer-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }
}
