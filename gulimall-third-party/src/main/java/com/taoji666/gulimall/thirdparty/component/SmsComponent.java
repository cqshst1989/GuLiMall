package com.taoji666.gulimall.thirdparty.component;

import com.taoji666.common.utils.HttpUtils;
import lombok.Data;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * 短信验证直接买阿里云的短信验证服务，但是直接从前端发请求到阿里云，不安全，因此还是要来服务端转发一下。
 *
 * 阿里云上有现成的java api，HttpUtils。
 *
 * 这里写一个短信组件给controller用， 由于涉及第三方服务，也不用Service+impl模式了
 *
 * @author: Taoji
 * @createTime: 2021-08-19 09:39
 **/

@ConfigurationProperties(prefix = "spring.cloud.alicloud.sms") //spring配置文件中的前缀
@Data
@Component
public class SmsComponent {

    //spring配置文件application.yml中，可配置的参数
    private String host;
    private String path;
    private String skin;
    private String sign;
    private String appcode;

    //传送手机号，和收到的验证码过来
    public void sendCode(String phone, String code) {
        String method = "GET";
        Map<String, String> headers = new HashMap<>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        Map<String, String> queries = new HashMap<String, String>();
        queries.put("code", code);
        queries.put("phone", phone);
        queries.put("skin", skin);
        queries.put("sign", sign);
        //JDK 1.8示例代码请在这里下载：  http://code.fegine.com/Tools.zip
        try {
            /**
             * 重要提示如下:
             * HttpUtils请从
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
             * 或者直接下载：
             * http://code.fegine.com/HttpUtils.zip
             * 下载
             *
             * 相应的依赖请参照
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
             * 相关jar包（非pom）直接下载：
             * http://code.fegine.com/aliyun-jar.zip
             */
            HttpResponse response = HttpUtils.doGet(host, path, method, headers, queries);
            //System.out.println(response.toString());如不输出json, 请打开这行代码，打印调试头部状态码。
            //状态码: 200 正常；400 URL无效；401 appCode错误； 403 次数用完； 500 API网管错误
            //获取response的body
            System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}