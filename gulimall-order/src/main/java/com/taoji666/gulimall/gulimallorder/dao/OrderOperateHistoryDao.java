package com.taoji666.gulimall.gulimallorder.dao;

import com.taoji666.gulimall.gulimallorder.entity.OrderOperateHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单操作历史记录
 * 
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-04 11:43:11
 */
@Mapper
public interface OrderOperateHistoryDao extends BaseMapper<OrderOperateHistoryEntity> {
	
}
