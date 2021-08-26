package com.taoji666.common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

//微服务之间传输 使用的 实体对象 TO，由于两个微服务同时会使用这个对象，因此TO一般放在公共类common中
@Data
public class SkuReductionTo {

    private Long skuId;
    private int fullCount;
    private BigDecimal discount;
    private int countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private int priceStatus;
    private List<MemberPrice> memberPrice;
}
