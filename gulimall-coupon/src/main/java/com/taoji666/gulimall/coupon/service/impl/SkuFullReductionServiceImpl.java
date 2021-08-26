package com.taoji666.gulimall.coupon.service.impl;

import com.taoji666.common.to.MemberPrice;
import com.taoji666.common.to.SkuReductionTo;
import com.taoji666.gulimall.coupon.entity.MemberPriceEntity;
import com.taoji666.gulimall.coupon.entity.SkuLadderEntity;
import com.taoji666.gulimall.coupon.service.MemberPriceService;
import com.taoji666.gulimall.coupon.service.SkuLadderService;
import com.taoji666.gulimall.coupon.dao.SkuFullReductionDao;
import com.taoji666.gulimall.coupon.entity.SkuFullReductionEntity;
import com.taoji666.gulimall.coupon.service.SkuFullReductionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    SkuLadderService skuLadderService;

    @Autowired
    MemberPriceService memberPriceService;



    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuReduction(SkuReductionTo reductionTo) { //product微服务传来的数据
        //1、// //5.4）、sku的优惠、满减等信息；gulimall_sms->sms_sku_ladder\sms_sku_full_reduction\sms_member_price
        //sms_sku_ladder 该数据表保存满减打折，会员价。由于属性名没有对应，没法用beanutils，只能一个一个提取出来赋值
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(reductionTo.getSkuId());
        skuLadderEntity.setFullCount(reductionTo.getFullCount());
        skuLadderEntity.setDiscount(reductionTo.getDiscount());
        skuLadderEntity.setAddOther(reductionTo.getCountStatus());
        if(reductionTo.getFullCount() > 0){
            skuLadderService.save(skuLadderEntity);
        }




        //2、sms_sku_full_reduction数据表的存储
        SkuFullReductionEntity reductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(reductionTo,reductionEntity);
        if(reductionEntity.getFullPrice().compareTo(new BigDecimal("0"))==1){
            this.save(reductionEntity);
        }


        //3、sms_member_price数据表 的存储
        List<MemberPrice> memberPrice = reductionTo.getMemberPrice();

        List<MemberPriceEntity> collect = memberPrice.stream().map(item -> {

            //由于属性名没有对应，没法用beanutils，只能一个一个提取出来赋值
            MemberPriceEntity priceEntity = new MemberPriceEntity();
            priceEntity.setSkuId(reductionTo.getSkuId());
            priceEntity.setMemberLevelId(item.getId());
            priceEntity.setMemberLevelName(item.getName());
            priceEntity.setMemberPrice(item.getPrice());
            priceEntity.setAddOther(1);//叠加其他优惠
            return priceEntity;
        }).filter(item->{
            return item.getMemberPrice().compareTo(new BigDecimal("0")) == 1;
        }).collect(Collectors.toList());

        memberPriceService.saveBatch(collect);
    }

}