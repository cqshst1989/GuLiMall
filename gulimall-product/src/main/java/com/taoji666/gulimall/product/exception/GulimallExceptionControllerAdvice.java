package com.taoji666.gulimall.product.exception;

import com.taoji666.common.exception.BizCodeEnume;
import com.taoji666.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 集中处理所有异常
 */
@Slf4j
//@ResponseBody //json的方式返回服务端
//@ControllerAdvice(basePackages = "com.atguigu.gulimall.product.controller")
//@RestControllerAdvice=@ResponseBody+@ControllerAdvice
@RestControllerAdvice(basePackages = "com.atguigu.gulimall.product.controller")
public class GulimallExceptionControllerAdvice {

    //处理所有数据校验相关的异常，校验失败的异常（@valid）封装成e,传递过来了
    //@ExceptionHandler表示可处理异常的级别，这个MethodArg，就是数据相关的异常
    @ExceptionHandler(value= MethodArgumentNotValidException.class)
    public R handleVaildException(MethodArgumentNotValidException e){
        log.error("数据校验出现问题{}，异常类型：{}",e.getMessage(),e.getClass());
        //通过传来的e，获取到具体异常
        BindingResult bindingResult = e.getBindingResult();

        Map<String,String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((fieldError)->{
            errorMap.put(fieldError.getField(),fieldError.getDefaultMessage());
        });


        //状态码放在一个公用的common子工程的BizCodeEnume枚举类里面
        //错误状态码，就用公用的枚举类相对应的属性和值，这里是数据校验的VAILID
        return R.error(BizCodeEnume.VAILD_EXCEPTION.getCode(),BizCodeEnume.VAILD_EXCEPTION.getMsg()).put("data",errorMap);
    }

    //前面是精确匹配数据方面的异常，这个是万能异常，顶级父类。所有异常都能来这里处理
    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable){

        log.error("错误：",throwable);
        //错误状态码，就用公用的枚举类相对应的属性和值，这里是其他的UNKNOW
        return R.error(BizCodeEnume.UNKNOW_EXCEPTION.getCode(),BizCodeEnume.UNKNOW_EXCEPTION.getMsg());
    }


}
