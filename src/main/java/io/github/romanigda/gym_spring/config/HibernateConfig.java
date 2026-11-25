package io.github.romanigda.gym_spring.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.romanigda.gym_spring.domain.User;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
public class HibernateConfig {

    private static final String[] HIBERNATE_PROPERTIES = {
            "hibernate.dialect",
            "hibernate.hbm2ddl.auto",
            "hibernate.show_sql",
            "hibernate.format_sql"
    };

    @Bean(destroyMethod = "close")
    public HikariDataSource dataSource(@Value("${db.url}") String url,
                                       @Value("${db.username}") String username,
                                       @Value("${db.password}") String password,
                                       @Value("${db.pool.max-size:10}") int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setAutoCommit(false);
        return new HikariDataSource(config);
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                // Existing databases were initialized outside Flyway, but still need V1.
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }

    @Bean
    @DependsOn("flyway")
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource, Environment environment) {
        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setPackagesToScan(User.class.getPackageName());
        factoryBean.setHibernateProperties(hibernateProperties(environment));
        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }

    private Properties hibernateProperties(Environment environment) {
        Properties properties = new Properties();
        for (String key : HIBERNATE_PROPERTIES) {
            String value = environment.getProperty(key);
            if (value != null) {
                properties.setProperty(key, value);
            }
        }
        properties.setProperty("hibernate.connection.provider_disables_autocommit", "true");
        return properties;
    }
}
