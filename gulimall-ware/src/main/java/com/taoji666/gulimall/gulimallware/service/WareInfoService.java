package com.taoji666.gulimall.gulimallware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.gulimallware.entity.WareInfoEntity;

import java.util.Map;

/**
 * 仓库信息
 *
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-04 11:50:55
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

