package com.taoji666.gulimall.ware.service.impl;

import com.taoji666.gulimall.ware.dao.WareInfoDao;
import com.taoji666.gulimall.ware.entity.WareInfoEntity;
import com.taoji666.gulimall.ware.service.WareInfoService;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        //新建一个查询条件wrapper
        QueryWrapper<WareInfoEntity> wareInfoEntityQueryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key"); //将Object 转换为 String

        if(!StringUtils.isEmpty(key)){
            wareInfoEntityQueryWrapper.eq("id",key).or()
                    .like("name",key)  //like 就是name = %key%
                    .or().like("address",key) //like 就是address = %key%
                    .or().like("areacode",key); //like 就是areacode = %key%
        }

        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params), //前端传来的参数带了分页条件
                wareInfoEntityQueryWrapper //查询条件
        );

        return new PageUtils(page);
    }

}