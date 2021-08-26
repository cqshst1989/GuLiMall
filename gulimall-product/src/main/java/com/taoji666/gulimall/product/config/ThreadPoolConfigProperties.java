package com.taoji666.gulimall.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Taoji
 * 有了这个类，就可以在spring配置文件中，写配置了
 * 主要在配置文件中配置 coreSize，maxSize，keepAliveTime 三个参数
 */
@ConfigurationProperties(prefix = "gulimall.thread") //在spring配置文件中，以gulimall.thread开头来配置以下参数
@Data
public class ThreadPoolConfigProperties {

    private Integer coreSize;

    private Integer maxSize;

    private Integer keepAliveTime;

}

