package com.taoji666.gulimall.product.config;

import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;



//直接复制mybatis-plus 官网内容
//这里创建分页对象，配置分页的通用参数
//至于具体给哪张数据表分页的配置，去那个数据表的XXserviceImp 写
@Configuration
@EnableTransactionManagement //允许开启事务，之后就可以在需要用事务的方法上添加@Transactional注解来开启事务
@MapperScan("com.atguigu.gulimall.product.dao")//扫描mapper接口的包
public class MyBatisConfig {

    //引入分页插件，前端管理产品页面，查出来的产品进行分页
    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        // 设置请求的页面大于最大页后操作， true调回到首页，false 继续请求  默认false
         paginationInterceptor.setOverflow(true);
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInterceptor.setLimit(1000);
        return paginationInterceptor;
    }
}
