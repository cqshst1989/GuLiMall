package com.taoji666.gulimall.product.config;


import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author: Taoji
 * @create: 2021-08-5 15:19
 * 单独写配置： 配置给redis中存放json，默认是存放java序列化的一堆看不懂的乱码，不适合跨语言，跨平台使用
 *
 * 配置原理
 *   CacheAutoConfiguration -> RedisCacheConfiguration -> 自动配置了RedisCacheManager
 *   ->初始化所有的缓存 -> 每个缓存决定使用什么配置
 *   如果redisCacheConfiguration有自定义配置，就使用自定义配置 （下面的配置类，就是配这个）
 *   如果没有自定义配置，才会使用默认配置
 *   想改缓存的配置，只需要给容器中放一个RedisCacheConfiguration即可，就会应用到当前的RedisCacheManager管理的所有缓存分区中
 *
 */

@EnableConfigurationProperties(CacheProperties.class) //让application.yml中的缓存相关配置生效
@Configuration
@EnableCaching
public class MyCacheConfig {

    /**
     * 配置文件的配置没有用上
     * 1. 原来和配置文件绑定的配置类为：@ConfigurationProperties(prefix = "spring.cache")
     *                                public class CacheProperties
     * <p>
     * 2. 要让他生效，要加上 @EnableConfigurationProperties(CacheProperties.class)
     */
    @Bean //返回一个RedisCacheConfiguration 加入spring容器，即可完成配置
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        // config = config.entryTtl();
        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));


        /**只写以上代码，配置文件不会生效的（我们在spring配置文件中写的ttl时间就不生效）
         * 原来和配置文件绑定的配置类为：@ConfigurationProperties(prefix = "spring.cache")
         *                                 public class CacheProperties
         * 现在要让配置文件中所有的配置都生效
         要加上 @EnableConfigurationProperties(CacheProperties.class)
         同时加入以下代码：
         */
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();

        //如果配置文件不为空，就去配置文件中获取数据
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixKeysWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }

}
