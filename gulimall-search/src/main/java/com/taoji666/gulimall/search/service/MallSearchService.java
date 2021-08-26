package com.taoji666.gulimall.search.service;

import com.taoji666.gulimall.search.vo.SearchParam;
import com.taoji666.gulimall.search.vo.SearchResult;

/**
 * @author: TaoJi
 * @date: 2021/8/9 10:03
 */
public interface MallSearchService {
    /**
     * 搜索
     *
     * @param param 检索的所有参数
     * @return 返回检索的结果SearchResult，里面包含页面需要的所有信息
     */
    SearchResult search(SearchParam param);
}
