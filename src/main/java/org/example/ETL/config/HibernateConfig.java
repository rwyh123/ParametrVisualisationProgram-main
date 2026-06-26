package org.example.ETL.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@PropertySource(value = "classpath:/application.properties")
public class HibernateConfig {

    @Value("${db.driver}") private String databaseDriver;
    @Value("${db.url}") private String databaseUrl;
    @Value("${db.username}") private String databaseUsername;
    @Value("${db.password}") private String databasePassword;

    @Value("${hibernate.dialect}") private String hibernateDialect;
    @Value("${hibernate.show_sql}") private String hibernateShowSql;
    @Value("${hibernate.hbm2ddl.auto}") private String hibernateHbm2ddlAuto;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(databaseDriver);
        ds.setUrl(databaseUrl);
        ds.setUsername(databaseUsername);
        ds.setPassword(databasePassword);
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource); // используй аргумент
        emf.setPackagesToScan("org.example.ETL.entities");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emf.setJpaProperties(hibernateProps());
        return emf;
    }


    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }

    private Properties hibernateProps() {
        Properties p = new Properties();
        p.setProperty("hibernate.dialect", hibernateDialect);
        p.setProperty("hibernate.show_sql", hibernateShowSql);
        p.setProperty("hibernate.hbm2ddl.auto", hibernateHbm2ddlAuto);
        return p;
    }

}
