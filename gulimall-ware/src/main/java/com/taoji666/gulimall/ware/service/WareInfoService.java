package com.taoji666.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.gulimall.ware.entity.WareInfoEntity;
import com.taoji666.gulimall.ware.vo.FareVo;

import java.util.Map;

/**
 * 仓库信息
 *
 * @author Taoji
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    FareVo getFare(Long addrId); //根据用户选择的收货地址计算运费
}

