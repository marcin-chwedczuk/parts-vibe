package app.partsvibe.infra.events.handling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class EventQueueExecutorConfig {
    @Bean(name = "eventQueueExecutor")
    public ThreadPoolTaskExecutor eventQueueExecutor(EventQueueDispatcherProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("event-queue-worker-");
        executor.setDaemon(true);
        executor.setCorePoolSize(properties.getThreadPoolSize());
        executor.setMaxPoolSize(properties.getThreadPoolSize());
        executor.setQueueCapacity(properties.getThreadPoolQueueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Bean(name = "eventQueueTimeoutScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService eventQueueTimeoutScheduler() {
        var threadFactory = Thread.ofPlatform()
                .name("event-queue-timeout-worker-", 1)
                .daemon(true)
                .factory();
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }
}
