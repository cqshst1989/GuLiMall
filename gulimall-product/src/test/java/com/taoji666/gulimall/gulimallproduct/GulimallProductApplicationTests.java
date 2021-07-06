package com.taoji666.gulimall.gulimallproduct;

import com.taoji666.gulimall.gulimallproduct.entity.BrandEntity;
import com.taoji666.gulimall.gulimallproduct.service.BrandService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallProductApplicationTests {
    @Autowired
    BrandService brandService;

    @Test
    void contextLoads() {
//
//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setName("Vivo");
//        brandService.save(brandEntity);
//        System.out.println("保存成功");

        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setBrandId(1L);
        brandEntity.setDescript("华为");
        brandService.updateById(brandEntity);

    }

}
