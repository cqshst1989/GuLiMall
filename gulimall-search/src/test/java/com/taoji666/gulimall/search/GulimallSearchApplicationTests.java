package com.taoji666.gulimall.search;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
class GulimallSearchApplicationTests {

    @Autowired
    RestHighLevelClient client;

    @Test
    public void contextLoads() {
        System.out.println(client);
    }

    /**
     * 测试存储数据到 es
     * source 方法用于保存数据，数据的格式为键值对形式的类型
     * - json 字符串
     * - Map
     * - XContentBuilder
     * - KV 键值对
     * - 实体类对象转json
     */
    @Test
    void indexData() throws IOException {  //在users索引下存储数据
        @Data
        class User{
            private String userName;
            private String gender;
            private Integer age;
        }


        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1"); //id 都要使用字符串，记得转成字符串


        User user = new User(); //new 一个对象，为解析做铺垫
        user.setUserName("张三");
        user.setAge(18);
        user.setGender("男");
        String jsonUser = JSONArray.toJSONString(user); //对象转换json

        //解析需要一个json
        indexRequest.source(jsonUser, XContentType.JSON); //传输json


        // 也可以直接写json，但是，你看下面各种+拼接，多难写啊
       /* indexRequest.source("{" +
                "\"user\":\"kimchy\"," +
                "\"postDate\":\"2013-01-30\"," +
                "\"message\":\"trying out Elasticsearch\"" +
                "}", XContentType.JSON);*/


        /* KV 键值对写法，一般不用，看下就是了
        indexRequest.source("username", "zhangsan", "age", 12, "address", "sz");  */

        // 同步执行操作，index就是索引数据，delete就是删除  第二个形参是配置类的配置设置（如果有特殊设置的话）
        IndexResponse index = client.index(indexRequest, com.taoji666.gulimall.search.config.GuliMallElasticSearchConfig.COMMON_OPTIONS);
        System.out.println(index); //打印响应数据
    }

    /**
     * 检索地址中带有 mill 的人员年龄分布和平均薪资
     * @throws IOException
     */
    @Test
    void searchData() throws IOException {
        // 1. 创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        // 指定索引
        searchRequest.indices("bank");
        // 指定 DSL 检索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 1.1 构建检索条件 address 包含 mill
        //searchSourceBuilder.query 查询
        //searchSourceBuilder.from 分页
        //searchSourceBuilder.aggregation 聚合

        searchSourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
        // 1.2 按照年龄值分布进行聚合  这个聚合取名叫ageAgg， 按照哪个属性进行聚合 age
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        searchSourceBuilder.aggregation(ageAgg);
        // 1.3 计算平均薪资
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        searchSourceBuilder.aggregation(balanceAvg);

        System.out.println("检索条件：" + searchSourceBuilder.toString());
        searchRequest.source(searchSourceBuilder);


        // 2. 执行检索, 获得响应
        SearchResponse searchResponse = client.search(searchRequest, com.taoji666.gulimall.search.config.GuliMallElasticSearchConfig.COMMON_OPTIONS);

        // 3. 分析结果  将检索到的数据封装成bean，或者map
        // 3.1 获取所有查到的记录
        SearchHits hits = searchResponse.getHits(); //这里是最外层命中记录
        SearchHit[] searchHits = hits.getHits(); //内部真正想要的hits
        for (SearchHit hit : searchHits) {
            // 数据字符串
            String jsonString = hit.getSourceAsString();
            System.out.println(jsonString);
            // 可以通过 json 转换成实体类对象  有网页工具帮忙直接转成 Account类的java对象
            // Account account = JSON.parseObject(jsonString, Account.class);
        }

        // 3.2 获取检索的分析信息(聚合数据等)
        Aggregations aggregations = searchResponse.getAggregations();
        // for (Aggregation aggregation : aggregations.asList()) {
        //     System.out.println("当前聚合名：" + aggregation.getName());
        // }
        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄：" + keyAsString + " 岁的有 " + bucket.getDocCount() + " 人");
        }

        Avg balanceAvg1 = aggregations.get("balanceAvg");
        System.out.println("平均薪资: " + balanceAvg1.getValue());
    }

}
