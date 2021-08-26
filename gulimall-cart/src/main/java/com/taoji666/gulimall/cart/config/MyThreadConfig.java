package com.taoji666.gulimall.cart.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Taoji
 * 自己new一个线程池，不用默认的，自定义线程池需要配置七大参数
 *
 * (1)@param corePoolSize
 * 池中一直保持的线程的数量，即使线程空闲。除非设置了 allowCoreThreadTimeOut
 * (2)@param maximumPoolSize
 * 池中允许的最大的线程数
 * (3)@param keepAliveTime
 * 当线程数大于核心线程数的时候，线程在最大多长时间没有接到新任务就会终止释放，最终线程池维持在 corePoolSize 大小
 * (4)@param unit
 * 时间单位
 * (5)@param workQueue
 * 阻塞队列，用来存储等待执行的任务，如果当前对线程的需求超过了 corePoolSize大小，就会放在这里等待空闲线程执行。
 * (6)@param threadFactory the factory to use when the executor
 * 创建线程的工厂，比如指定线程名等
 * (7)@param handler
 * 拒绝策略，如果线程满了，线程池就会使用拒绝策略
 *
 */
@EnableConfigurationProperties(ThreadPoolConfigProperties.class) //用导入配置文件配置类，之后可以直接反射配置文件中的值
@Configuration
public class MyThreadConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigProperties pool) {
        return new ThreadPoolExecutor(
                pool.getCoreSize(), //从配置文件中拿到CoreSize
                pool.getMaxSize(), //从配置文件中拿到MaxSize
                pool.getKeepAliveTime(), //从配置文件中拿到KeepAliveTime
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100000),
                Executors.defaultThreadFactory(), //使用默认线程工厂
                new ThreadPoolExecutor.AbortPolicy() //放不下的线程就抛弃
        );
    }
}

