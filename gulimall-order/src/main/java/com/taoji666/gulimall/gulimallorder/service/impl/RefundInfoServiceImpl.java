package com.taoji666.gulimall.gulimallorder.service.impl;

import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import com.taoji666.gulimall.gulimallorder.dao.RefundInfoDao;
import com.taoji666.gulimall.gulimallorder.entity.RefundInfoEntity;
import com.taoji666.gulimall.gulimallorder.service.RefundInfoService;


@Service("refundInfoService")
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoDao, RefundInfoEntity> implements RefundInfoService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<RefundInfoEntity> page = this.page(
                new Query<RefundInfoEntity>().getPage(params),
                new QueryWrapper<RefundInfoEntity>()
        );

        return new PageUtils(page);
    }

}