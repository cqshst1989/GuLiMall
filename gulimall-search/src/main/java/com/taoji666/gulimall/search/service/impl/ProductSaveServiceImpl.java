package com.taoji666.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.taoji666.common.to.es.SkuEsModel;
import com.taoji666.gulimall.search.config.GuliMallElasticSearchConfig;
import com.taoji666.gulimall.search.constant.EsConstant;
import com.taoji666.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service("productSaveService")
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    private RestHighLevelClient restHighLevelClient; //用这个esclient来操作

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        //1.在es中建立索引，建立好映射关系（doc/json/new_product-mapping.json ）

        //2. 在ES中保存这些数据。  数据已经组装好了，只需要通过es客户端操作这些数据保存进es（linux中）就好

        //bulkRequest是后面bulk方法的形参1，先构造出来并且设置好参数
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel model : skuEsModels) { //for(类型 类型对应的形参 : 要遍历的集合)
            //构造保存请求
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX); //EsConstant中设置好的索引传入
            indexRequest.id(model.getSkuId().toString());//索引中，添加id  es只要字符串，因此要转成字符串
            String s = JSON.toJSONString(model);//用json工具，把已经从数据库中查好的skuEsModel对象转换成json
            indexRequest.source(s, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        //bulk方法，批量保存数据 形参bulk(BulkRequest bulkRequest, 配置类中的配置)
        //保存完成后，还会将bulk返回，封装了很多关于操作结果的方法，比如错误处理方法
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GuliMallElasticSearchConfig.COMMON_OPTIONS);

        //如果有错误，一般都没得错误
        boolean b = bulk.hasFailures();

        List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());

        log.info("商品上架完成：{},返回数据:{}", collect,bulk.toString());

        return b;
    }
}
