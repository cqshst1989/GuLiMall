package com.taoji666.gulimall.gulimallcoupon.dao;

import com.taoji666.gulimall.gulimallcoupon.entity.CouponHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券领取历史记录
 * 
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-04 11:15:33
 */
@Mapper
public interface CouponHistoryDao extends BaseMapper<CouponHistoryEntity> {
	
}
