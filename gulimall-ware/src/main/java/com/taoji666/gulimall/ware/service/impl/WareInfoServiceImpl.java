package com.taoji666.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.taoji666.common.utils.R;
import com.taoji666.gulimall.ware.dao.WareInfoDao;
import com.taoji666.gulimall.ware.entity.WareInfoEntity;
import com.taoji666.gulimall.ware.feign.MemberFeignService;
import com.taoji666.gulimall.ware.service.WareInfoService;
import com.taoji666.gulimall.ware.vo.FareVo;
import com.taoji666.gulimall.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {
    @Autowired
    MemberFeignService memberFeignService;

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
    /*
    * 根据选择的收货地址，计算运费
    *
    * */
    @Override
    public FareVo getFare(Long addrId) {
        FareVo fareVo = new FareVo();

        //通过addrId，远程获取到该收货地址的详细信息
        R info = memberFeignService.info(addrId);
        if (info.getCode() == 0) {
            //将R快速转换成 MemberAddressVo   key就是远程调用时，put进R的对象名称
            MemberAddressVo address = info.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {
            });
            fareVo.setAddress(address);

            //假装  电话号的最后两位是邮费
            String phone = address.getPhone();
            // substring 截取 手机号码 最后两位数字
            String fare = phone.substring(phone.length() - 2, phone.length());
            fareVo.setFare(new BigDecimal(fare));
        }
        return fareVo;
    }

}