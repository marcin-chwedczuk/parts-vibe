package app.partsvibe.infra.events.handling;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class EventQueueExecutorConfig {
    @Bean(name = "eventQueueExecutor")
    public ThreadPoolTaskExecutor eventQueueExecutor(EventQueueWorkerProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("event-handling-worker-");
        executor.setDaemon(true);
        executor.setCorePoolSize(properties.getPoolSize());
        executor.setMaxPoolSize(properties.getPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
