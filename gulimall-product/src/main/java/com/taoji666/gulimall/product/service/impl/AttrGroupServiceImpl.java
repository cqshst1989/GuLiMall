package com.taoji666.gulimall.product.service.impl;

import com.taoji666.gulimall.product.entity.AttrEntity;
import com.taoji666.gulimall.product.service.AttrService;
import com.taoji666.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.taoji666.gulimall.product.dao.AttrGroupDao;
import com.taoji666.gulimall.product.entity.AttrGroupEntity;
import com.taoji666.gulimall.product.service.AttrGroupService;
import com.taoji666.gulimall.product.vo.SpuItemAttrGroupVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import org.springframework.util.StringUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }
    //参数由AttrGroupController中的@RequestParam 和 @PathVariable 获取前端的json 后传递过来
    //分别代表URL的 请求值，和路径值
    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        //key是模糊检索的关键字，封装进了params里面，现在将key取出来
        String key = (String) params.get("key");
        //select * from pms_attr_group where catelog_id=? and (attr_group_id=key or attr_group_name like %key%)
        //wrapper是在构造查询条件
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if(!StringUtils.isEmpty(key)){ //如果这个key不为空
            wrapper.and((obj)->{ //那我们就用传过来的 id' 或者 name 做事情
                obj.eq("attr_group_id",key).or().like("attr_group_name",key);
            });
        }

        //如果前端传来的catelogId=0 则查询数据表所有

        if( catelogId == 0){  //page是serviceImpl里面的现成方法，直接拿来用。this显然是父类了，本类都找不到page方法
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);

        }else {

            wrapper.eq("catelog_id",catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        }

    }

    /**
     * 根据分类id查出所有的分组以及这些组里面的属性
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
            //com.atguigu.gulimall.product.vo
        //1、查询分组信息  这些是本service对应的数据表里本身就有的，直接用现有方法查
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));

        //2、查询所有属性  要用别人的service。查出来以后和自己本身的装配在一起
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map(group -> {
            AttrGroupWithAttrsVo attrsVo = new AttrGroupWithAttrsVo();
            BeanUtils.copyProperties(group,attrsVo);
            List<AttrEntity> attrs = attrService.getRelationAttr(attrsVo.getAttrGroupId());
            attrsVo.setAttrs(attrs);
            return attrsVo;
        }).collect(Collectors.toList());

        return collect;


    }

    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        //1、查出当前spu对应的所有属性的分组信息以及当前分组下的所有属性对应的值
        AttrGroupDao baseMapper = this.getBaseMapper();

        //需要自己写 三表联合查询 getAttrGroupWithAttrsBySpuId(spuId,catalogId)方法
        return baseMapper.getAttrGroupWithAttrsBySpuId(spuId,catalogId);
    }

}