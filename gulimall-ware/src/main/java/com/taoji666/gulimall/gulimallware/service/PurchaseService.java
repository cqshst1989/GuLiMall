package com.taoji666.gulimall.gulimallware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.gulimallware.entity.PurchaseEntity;

import java.util.Map;

/**
 * 采购信息
 *
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-04 11:50:55
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

