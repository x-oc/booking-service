package ru.vova.airbnb.config;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import ru.vova.airbnb.property.repository.PropertyRepository;
import ru.vova.airbnb.repository.BookingRepository;
import ru.vova.airbnb.repository.UserRepository;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class MultiDbJpaConfig {

    @Bean(name = "coreDataSource", initMethod = "init", destroyMethod = "close")
    @Primary
    public DataSource coreDataSource(
            @Value("${app.datasource.core.xa-class-name:org.postgresql.xa.PGXADataSource}") String xaClassName,
            @Value("${app.datasource.core.url}") String url,
            @Value("${app.datasource.core.username}") String username,
            @Value("${app.datasource.core.password}") String password) {
        return buildAtomikosDataSource("core-db", xaClassName, url, username, password);
    }

    @Bean(name = "propertyDataSource", initMethod = "init", destroyMethod = "close")
    public DataSource propertyDataSource(
            @Value("${app.datasource.property.xa-class-name:org.postgresql.xa.PGXADataSource}") String xaClassName,
            @Value("${app.datasource.property.url}") String url,
            @Value("${app.datasource.property.username}") String username,
            @Value("${app.datasource.property.password}") String password) {
        return buildAtomikosDataSource("property-db", xaClassName, url, username, password);
    }

    @Bean
    @Primary
    public JpaVendorAdapter jpaVendorAdapter(
            @Value("${app.jpa.show-sql:true}") boolean showSql) {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(showSql);
        return adapter;
    }

    @Bean(name = "coreEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean coreEntityManagerFactory(
            @Qualifier("coreDataSource") DataSource coreDataSource,
            JpaVendorAdapter jpaVendorAdapter,
            @Value("${app.jpa.ddl-auto:update}") String ddlAuto,
            @Value("${app.jpa.dialect:org.hibernate.dialect.PostgreSQLDialect}") String dialect,
            @Value("${app.jpa.jdbc-time-zone:Europe/Moscow}") String jdbcTimeZone,
            @Value("${app.jpa.format-sql:true}") boolean formatSql) {
        return buildEntityManagerFactory(
                "core-pu",
                coreDataSource,
                jpaVendorAdapter,
                ddlAuto,
                dialect,
                jdbcTimeZone,
                formatSql,
                "ru.vova.airbnb.entity"
        );
    }

    @Bean(name = "propertyEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean propertyEntityManagerFactory(
            @Qualifier("propertyDataSource") DataSource propertyDataSource,
            JpaVendorAdapter jpaVendorAdapter,
            @Value("${app.jpa.ddl-auto:update}") String ddlAuto,
            @Value("${app.jpa.dialect:org.hibernate.dialect.PostgreSQLDialect}") String dialect,
            @Value("${app.jpa.jdbc-time-zone:Europe/Moscow}") String jdbcTimeZone,
            @Value("${app.jpa.format-sql:true}") boolean formatSql) {
        return buildEntityManagerFactory(
                "property-pu",
                propertyDataSource,
                jpaVendorAdapter,
                ddlAuto,
                dialect,
                jdbcTimeZone,
                formatSql,
                "ru.vova.airbnb.property.entity"
        );
    }

    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("coreDataSource") DataSource coreDataSource) {
        return new JdbcTemplate(coreDataSource);
    }

    private LocalContainerEntityManagerFactoryBean buildEntityManagerFactory(
            String persistenceUnitName,
            DataSource dataSource,
            JpaVendorAdapter jpaVendorAdapter,
            String ddlAuto,
            String dialect,
            String jdbcTimeZone,
            boolean formatSql,
            String packageToScan) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setPersistenceUnitName(persistenceUnitName);
        emf.setJtaDataSource(dataSource);
        emf.setPackagesToScan(packageToScan);
        emf.setJpaVendorAdapter(jpaVendorAdapter);

        Properties jpaProps = new Properties();
        jpaProps.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        jpaProps.setProperty("hibernate.dialect", dialect);
        jpaProps.setProperty("hibernate.jdbc.time_zone", jdbcTimeZone);
        jpaProps.setProperty("hibernate.format_sql", String.valueOf(formatSql));
        jpaProps.setProperty(
            "hibernate.physical_naming_strategy",
            "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
        );
        jpaProps.setProperty("hibernate.transaction.coordinator_class", "jta");
        jpaProps.setProperty("hibernate.transaction.jta.platform",
                "org.hibernate.engine.transaction.jta.platform.internal.AtomikosJtaPlatform");
        emf.setJpaProperties(jpaProps);
        return emf;
    }

    private DataSource buildAtomikosDataSource(String resourceName,
                                               String xaClassName,
                                               String url,
                                               String username,
                                               String password) {
        Properties xaProperties = new Properties();
        xaProperties.setProperty("url", url);
        xaProperties.setProperty("URL", url);
        xaProperties.setProperty("user", username);
        xaProperties.setProperty("User", username);
        xaProperties.setProperty("password", password);
        xaProperties.setProperty("Password", password);

        AtomikosDataSourceBean dataSourceBean = new AtomikosDataSourceBean();
        dataSourceBean.setUniqueResourceName(resourceName);
        dataSourceBean.setXaDataSourceClassName(xaClassName);
        dataSourceBean.setXaProperties(xaProperties);
        dataSourceBean.setMinPoolSize(1);
        dataSourceBean.setMaxPoolSize(10);
        dataSourceBean.setBorrowConnectionTimeout(30);
        dataSourceBean.setTestQuery("SELECT 1");
        return dataSourceBean;
    }

    @Configuration
    @EnableJpaRepositories(
            basePackageClasses = {BookingRepository.class, UserRepository.class},
            entityManagerFactoryRef = "coreEntityManagerFactory",
            transactionManagerRef = "transactionManager"
    )
    static class CoreRepositoriesConfig {
    }

    @Configuration
    @EnableJpaRepositories(
            basePackageClasses = {PropertyRepository.class},
            entityManagerFactoryRef = "propertyEntityManagerFactory",
            transactionManagerRef = "transactionManager"
    )
    static class PropertyRepositoriesConfig {
    }
}
