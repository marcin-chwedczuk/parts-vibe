package app.partsvibe.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceRoutingConfig {
    private static final String READ_KEY = "read";
    private static final String WRITE_KEY = "write";

    @Bean
    @ConfigurationProperties("app.datasource.write")
    public HikariDataSource writeDataSource() {
        return new HikariDataSource();
    }

    @Bean
    @ConfigurationProperties("app.datasource.read")
    public HikariDataSource readDataSource() {
        return new HikariDataSource();
    }

    @Bean
    @Primary
    public DataSource routingDataSource(DataSource writeDataSource, DataSource readDataSource) {
        Map<Object, Object> targets = new HashMap<>();
        targets.put(WRITE_KEY, writeDataSource);
        targets.put(READ_KEY, readDataSource);

        RoutingDataSource routingDataSource = new RoutingDataSource(READ_KEY, WRITE_KEY);
        routingDataSource.setDefaultTargetDataSource(writeDataSource);
        routingDataSource.setTargetDataSources(targets);
        return routingDataSource;
    }
}
