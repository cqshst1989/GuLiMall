package com.taoji666.gulimall.gulimallware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.gulimallware.entity.PurchaseDetailEntity;

import java.util.Map;

/**
 * 
 *
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-04 11:50:55
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

