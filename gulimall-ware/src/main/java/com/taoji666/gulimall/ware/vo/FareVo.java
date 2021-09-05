package com.taoji666.gulimall.ware.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    private MemberAddressVo address; //收货地址
    private BigDecimal fare;
}
