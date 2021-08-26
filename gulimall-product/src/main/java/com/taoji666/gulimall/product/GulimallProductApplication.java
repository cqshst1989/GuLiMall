package com.taoji666.gulimall.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 1、整合MyBatis-Plus
 *      1）、导入依赖
 *      <dependency>
 *             <groupId>com.baomidou</groupId>
 *             <artifactId>mybatis-plus-boot-starter</artifactId>
 *             <version>3.2.0</version>
 *      </dependency>
 *      2）、配置
 *          1、配置数据源；
 *              1）、导入数据库的驱动。https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-versions.html
 *              2）、在application.yml配置数据源相关信息
 *          2、配置MyBatis-Plus；
 *              1）、使用@MapperScan
 *              2）、告诉MyBatis-Plus，sql映射文件位置
 *
 * 2、逻辑删除
 *  1）、配置全局的逻辑删除规则（省略）
 *  2）、配置逻辑删除的组件Bean（省略）
 *  3）、给Bean加上逻辑删除注解@TableLogic
 *
 * 3、JSR303
 *   1）、给Bean添加校验注解:javax.validation.constraints，并定义自己的message提示
 *   2)、开启校验功能@Valid
 *      效果：校验错误以后会有默认的响应；
 *   3）、给校验的bean后紧跟一个BindingResult，就可以获取到校验的结果
 *   4）、分组校验（多场景的复杂校验） groups
 *         1)、	@NotBlank(message = "品牌名必须提交",groups = {AddGroup.class,UpdateGroup.class})
 *          给校验注解标注什么情况需要进行校验
 *         2）、@Validated({AddGroup.class})
 *         3)、默认没有指定分组的校验注解@NotBlank，在分组校验情况@Validated({AddGroup.class})下不生效，只会在@Validated生效；
 *
 *   5）、自定义校验
 *      1）、编写一个自定义的校验注解
 *      2）、编写一个自定义的校验器 ConstraintValidator
 *      3）、关联自定义的校验器和自定义的校验注解
         *      @Documented
         * @Constraint(validatedBy = { ListValueConstraintValidator.class【可以指定多个不同的校验器，适配不同类型的校验】 })
         * @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
         * @Retention(RUNTIME)
         * public @interface ListValue {
 *
 * 4、统一的异常处理
 * @ControllerAdvice
 *  1）、编写异常处理类，使用@ControllerAdvice。
 *  2）、使用@ExceptionHandler标注方法可以处理的异常。
 *
 *  5、模板引擎
 *  1）、引入 thymeleaf-starter
 *  2）、静态资源都放在static文件夹下，就可以按照路径直接访问
 *  3）、页面放在templates下，也可直接访问
 *  4)、页面修改不重启服务器实时更新
 *    1、引入dev-tools依赖
 *    2、修改完页面后 ctrl + shift + F9 重新自动编译页面（IDEA的 Build下）。特别涉及代码配置，为了避免bug，还是重启下

 *  6)、缓存SpringCache (springCache只是抽取缓存通用代码，实际还是用redis来缓存)
 *    1、pom中引入依赖 SpringCache + Redis
 *    2、配置文件写配置 主要是告诉springcache 我用的是redis
 *    3、主配置类 或者 缓存配置类 上开启缓存功能 @EnableCaching （本项目放在缓存配置类MyCacheConfig.java中）
 *    4、直接使用缓存注解，完成操作
 *      1、@Cacheable（方法上）  ：将当前方法的返回值保存到缓存中；（如果缓存中有该结果，就不调用该方法） 逻辑：查找缓存 - 有就返回 -没有就执行方法体 - 将结果缓存起来；
 *      2、@CachePut ：和@Cacheable一样，都是将方法的返回值添加缓存。但差异是，要先执行方法体。有结果就缓存，没有就算了 逻辑：执行方法体 - 将结果缓存起来
 *       总结： @Cacheable 适用于查询数据的方法，@CachePut 适用于更新数据的方法。
 *
 *      3、@CacheEvict  : 触发将数据从缓存删除的操作,但是只删除一个或者所有；
 *      4、@Cacheing：组合以上多个操作，比如可以弥补@CacheEvict只能删除一个的不足，删除多个，就用这个注解组合，详情见updateCascade方法；
 *      5、@CacheConfig：在类级别共享缓存的相同配置；
 *
 */


@EnableFeignClients(basePackages = "com.taoji666.gulimall.product.feign") //指定扫描的包，不指定也会自动扫描主配置类及其下面的包
@EnableDiscoveryClient
@MapperScan("com.taoji666.gulimall.product.dao")
@SpringBootApplication
public class GulimallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallProductApplication.class, args);
    }

}
