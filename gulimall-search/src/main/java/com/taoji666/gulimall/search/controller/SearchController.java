package com.taoji666.gulimall.search.controller;

import com.taoji666.gulimall.search.service.MallSearchService;
import com.taoji666.gulimall.search.vo.SearchParam;
import com.taoji666.gulimall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    private MallSearchService mallSearchService;

    /**
     * 用 页面提交过来的所有请求参数 去ES中查结果 并将结果封装成我们指定的对象
     *
     * @param param
     * @return SearchResult 自己封装的vo
     */
    @GetMapping(value = "/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request) { //特別注意，请求参数和路径变量是两回事。这里看不到请求参数哈

        //通过request取出URL末尾的查询条件，并赋值给param的 _queryString属性
        param.set_queryString(request.getQueryString());

        //根据传递来的页面的查询参数，去es中检索商品.
        SearchResult result = mallSearchService.search(param);

        model.addAttribute("result", result);

        return "list";  //@Controller 会解析 去list.html
    }
}
