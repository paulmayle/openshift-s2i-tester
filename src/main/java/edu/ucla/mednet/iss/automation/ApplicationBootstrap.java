package edu.ucla.mednet.iss.automation;

import java.util.concurrent.Executor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableScheduling

@EnableConfigurationProperties({
    FileStorageProperties.class
})
@EnableAsync(proxyTargetClass=true)

public class ApplicationBootstrap {

    public static void main(final String[] args) throws Exception {
        SpringApplication.run(ApplicationBootstrap.class, args);
    }


    @Bean(name="processExceutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(6);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("CleanUp-");
        executor.initialize();
        return executor;
    }
}
