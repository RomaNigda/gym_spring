package io.github.romanigda.gym_spring.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class HibernateConfigTest {
    @Test
    void configuresFlywayForNewAndExistingDatabases() {
        // Arrange
        DataSource dataSource = mock(DataSource.class);
        HibernateConfig config = new HibernateConfig();

        // Act
        Flyway flyway = config.flyway(dataSource);

        // Assert
        assertSame(dataSource, flyway.getConfiguration().getDataSource());
        assertTrue(flyway.getConfiguration().isBaselineOnMigrate());
        assertEquals("0", flyway.getConfiguration().getBaselineVersion().getVersion());
        assertArrayEquals(new Location[]{new Location("classpath:db/migration")},
                flyway.getConfiguration().getLocations());
    }

    @Test
    void initializesFlywayBeforeHibernate() throws NoSuchMethodException {
        // Arrange
        Method flywayMethod = HibernateConfig.class.getMethod("flyway", DataSource.class);
        Method sessionFactoryMethod = HibernateConfig.class.getMethod(
                "sessionFactory", DataSource.class, Environment.class);

        // Act
        Bean flywayBean = flywayMethod.getAnnotation(Bean.class);
        DependsOn sessionFactoryDependencies = sessionFactoryMethod.getAnnotation(DependsOn.class);

        // Assert
        assertNotNull(flywayBean);
        assertEquals("migrate", flywayBean.initMethod());
        assertNotNull(sessionFactoryDependencies);
        assertArrayEquals(new String[]{"flyway"}, sessionFactoryDependencies.value());
    }
}
