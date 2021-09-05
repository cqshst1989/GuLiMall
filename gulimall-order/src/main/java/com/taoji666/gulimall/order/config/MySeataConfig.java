package com.taoji666.gulimall.order.config;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/*
* 使用全局事务需要使用  数据源代理包装 默认的HikariDataSource数据源
*
* 需要使用全局事务的微服务 order、ware、product
* */
@Configuration
public class MySeataConfig {
    @Autowired
    DataSourceProperties dataSourceProperties;


    @Bean
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {

        HikariDataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        if (StringUtils.hasText(dataSourceProperties.getName())) {
            dataSource.setPoolName(dataSourceProperties.getName());
        }

        return new DataSourceProxy(dataSource);
    }
}
