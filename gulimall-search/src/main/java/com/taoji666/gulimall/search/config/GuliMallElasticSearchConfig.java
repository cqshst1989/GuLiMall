package com.taoji666.gulimall.search.config;


import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: Taoji
 * @date: 2021/7/24 15:22
 */
@Configuration
public class GuliMallElasticSearchConfig {
    /**
     * 1、导入依赖
     * 2、编写配置，给容器中注入RestHighLevelClient
     *
     * 参考官方API完成操作：
     * https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-low-usage-requests.html
     */

    //通用设置项，设置的时候解开注释
    public static final RequestOptions COMMON_OPTIONS;

    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        // builder.addHeader("Authorization", "Bearer " + TOKEN);
        // builder.setHttpAsyncResponseConsumerFactory(
        //         new HttpAsyncResponseConsumerFactory
        //                 .HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
    }

    @Bean
    public RestHighLevelClient esRestClient() {
        //直接去官方文档可以看到此配置
        return new RestHighLevelClient(
                RestClient.builder(  //指定es服务端的ip地址，端口号，协议名
                        new HttpHost("192.168.163.131", 9200, "http")));
    }
}
