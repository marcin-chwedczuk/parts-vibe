package app.partsvibe.infra.events.handling;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class EventQueueExecutorConfig {
    @Bean(name = "eventQueueExecutor")
    public ThreadPoolTaskExecutor eventQueueExecutor(EventQueueDispatcherProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("event-handling-worker-");
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
        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("event-timeout-scheduler-" + threadCounter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }
}
