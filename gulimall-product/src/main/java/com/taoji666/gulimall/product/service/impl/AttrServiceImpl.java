package com.taoji666.gulimall.product.service.impl;

import com.taoji666.common.constant.ProductConstant;
import com.taoji666.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.taoji666.gulimall.product.dao.AttrGroupDao;
import com.taoji666.gulimall.product.dao.CategoryDao;
import com.taoji666.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.taoji666.gulimall.product.entity.AttrGroupEntity;
import com.taoji666.gulimall.product.entity.CategoryEntity;
import com.taoji666.gulimall.product.service.CategoryService;
import com.taoji666.gulimall.product.vo.AttrGroupRelationVo;
import com.taoji666.gulimall.product.vo.AttrRespVo;
import com.taoji666.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.taoji666.gulimall.product.dao.AttrDao;
import com.taoji666.gulimall.product.entity.AttrEntity;
import com.taoji666.gulimall.product.service.AttrService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;



    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity(); //这个是对应数据库的PO
//        attrEntity.setAttrName(attr.getAttrName());
        BeanUtils.copyProperties(attr,attrEntity);//spring自带黑科技，快速将前端发来的属性值VO，给数据库PO，属性名必须一样才行
        //1、保存基本数据
        this.save(attrEntity);
        //2、去关联关系数据表，同步冗余字段 attrgroupID  和 attrID
        //基本属性需要关联（有关联数据表），销售属性不需要（没设计关联数据表），写一个枚举来保存这两种情况。然后判定是否关联
        if(attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId()!=null){
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();//new出关联关系数据表
            relationEntity.setAttrGroupId(attr.getAttrGroupId());//设置好关联字段 attrgroupid
            relationEntity.setAttrId(attrEntity.getAttrId());//设置好关联字段 attrid
            relationDao.insert(relationEntity); //插入数据库
        }


    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().eq("attr_type","base".equalsIgnoreCase(type)?ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode():ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        //不为0的时候 按照ID查询
        if(catelogId != 0){
            queryWrapper.eq("catelog_id",catelogId);
        }
        //前端传来的参数中的key 取出来（即检索条件）
        String key = (String) params.get("key");
        //如果检索条件不为空
        if(!StringUtils.isEmpty(key)){
            //可以同时按照attr_id 或者 attr_name 来索引（key可以为name 也可以为 id）
            queryWrapper.and((wrapper)->{
                wrapper.eq("attr_id",key).or().like("attr_name",key);
            });
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords(); //将记录读取出来，准备通过流的方式封装进vo实体
        //map方法可以将一个对象转换成另一个对象。这里将attrEntity对象转AttrRespVo，同时也给该vo新属性赋值
        //返回的对象，就是要转换成的对象
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            //首先将实体里面对应的属性 给 attrRespVo（RespVo是返回给前端的，但是还有两个新的属性）
            //依然先用spring的黑科技BeanUtils，来先封装现成的属性
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            //1、设置分类和分组的名字
            if("base".equalsIgnoreCase(type)){
                //根据属性表的id，查出对应的  属性-分组关系表 的数据 attrId
                AttrAttrgroupRelationEntity attrId = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));

               //如果有数据，就要给vo赋值，先通过 属性-分组 关系数据表 找到对应的分组数据表
                if (attrId != null && attrId.getAttrGroupId()!=null) {
                    //通过ID查找到对应的分组数据表，从分组数据表里面找 vo 想要的GroupName
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrId.getAttrGroupId());
                    //这下可以给vo赋值了
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }

            }

            //2、同理，从目录数据表中找到分类名称 赋值给 vo的对应字段 catelogName 返回给前端
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }
            //这下attrRespVO里面就既有实体里面该有的属性，也增加了自己的属性了
            return attrRespVo;
        }).collect(Collectors.toList()); //转换成attrRespVo对象后，再用list重装
        //将这个list给分页工具
        pageUtils.setList(respVos);
        return pageUtils;
    }
    //这个微服务会被远程调用，但由于远程调用效率很低，因此将他的返回值也送去Es
    @Cacheable(value = "attr",key = "'attrinfo:'+#root.args[0]") //#root.args[0]第一个参数。key名为attrinfo:传来的实参
    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo respVo = new AttrRespVo(); //先创建一个空参的respVo对象，然后慢慢加内容，最后返回这个对象
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity,respVo); //spring工具类快速copy现有属性到respvo


        //新增属性，就一步一步设置
        //基本属性需要关联（有关联数据表），销售属性不需要（没设计关联数据表），写一个枚举来保存这两种情况。然后判定是否关联
        if(attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            //1、新增分组信息
            //通过关联表 来找分组表
            AttrAttrgroupRelationEntity attrgroupRelation = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if(attrgroupRelation!=null){
                respVo.setAttrGroupId(attrgroupRelation.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupRelation.getAttrGroupId());
                if(attrGroupEntity!=null){
                    respVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }


        //2、设置分类信息
        Long catelogId = attrEntity.getCatelogId();
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        respVo.setCatelogPath(catelogPath);


        //3、继续设置分类名字
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if(categoryEntity!=null){
            respVo.setCatelogName(categoryEntity.getName());
        }


        return respVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        //首先用spring工具BeanUtils快速将现有的值，将前端传来的vo快速赋值给attrentity
        BeanUtils.copyProperties(attr,attrEntity);
        //先把attrEntity数据表更新了
        this.updateById(attrEntity);

        //分组关系表也冗余设置了相关字段，一起更新
        if(attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){

            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();

            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attr.getAttrId());

            Integer count = relationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            if(count>0){

                relationDao.update(relationEntity,new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id",attr.getAttrId()));

            }else{
                relationDao.insert(relationEntity);
            }
        }


    }

    /**
     * 根据分组id查找关联的所有基本属性  因为刚好和实体匹配，就不需要vo了
     * 这里又是通过中间表去获取属性，stream.map 重新将查到的所有id 封装成list，该list作为查询条件，查询属性
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        //通过groupid 查找  关系表relationentity  对应的很多行List，
        List<AttrAttrgroupRelationEntity> entities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));
        //通过流将关系表 转换 为 集合list   map回调自己，传入参数就是entities（关系表的ID）
        List<Long> attrIds = entities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        if(attrIds == null || attrIds.size() == 0){
            return null;
        }
        //用刚刚的list集合attrIds 作为查询条件，查询属性表
        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);
        return (List<AttrEntity>) attrEntities;
    }
    //点击移除，该属性 和 这个属性分组就不再有关系
    //谷粒商城文档12 删除属性与分组的关联关系
    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) { //前端传过来的数据是要解除关系的（attrid 和attrgroup） 封装成数组
        //relationDao.delete(new QueryWrapper<>().eq("attr_id",1L).eq("attr_group_id",1L));
        //前端传来的是数组，先将数组转换为list集合。attrid 和 attrgroup刚好又在一张attrgrouprelation数据表上
        List<AttrAttrgroupRelationEntity> entities = Arrays.asList(vos).stream().map((item) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity); //将前端传来的传来的要解除关系的attrid 和 attrgroup 复制进实体（为啥不直接进实体？）
            return relationEntity;
        }).collect(Collectors.toList());
        relationDao.deleteBatchRelation(entities);//从数据表中删除attrid 和 groupid 同时所在的行。只能自己写sql语句了
    }

    /**
     * 获取当前分组没有关联的所有属性
     * @param params
     * @param attrgroupId
     * @return
     * 谷粒商城文档13、获取前端传来的某个属性分组（attrgroupId）没有关联的其他属性
     * 不同属性分组 关联的属性 是不一样的
     * 比如 分组1 关联了属性a，b
     * 分组2，就不能关联属性a，b，要不你的属性分组就有问题
     *
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        //1、当前分组只能关联自己所属的分类里面的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();//获取到商品分类ID（tree结构那个），用于后面查找当前分类下的 属性分组
        //2、当前分组只能关联别的分组没有引用的属性
        //2.1)、当前分类下的其他分组属性id
        List<AttrGroupEntity> group = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<Long> collect = group.stream().map(item -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());

        //2.2)、这些分组关联的属性
        List<AttrAttrgroupRelationEntity> groupId = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", collect));
        List<Long> attrIds = groupId.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        //2.3)、从当前分类的所有属性中移除这些属性；
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId).eq("attr_type",ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if(attrIds!=null && attrIds.size()>0){
            wrapper.notIn("attr_id", attrIds);
        }
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((w)->{
                w.eq("attr_id",key).or().like("attr_name",key);
            });
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        PageUtils pageUtils = new PageUtils(page);

        return pageUtils;
    }

//    在指定的所有属性集合里面，挑出检索属性（是否检索标记为1的）

    @Override
    public List<Long> selectSearchAttrs(List<Long> attrIds) {


        //去Dao中写这个 select * from `pms_attr` WHERE attr_id IN (?) AND search_type =1
        return baseMapper.selectSearchAttrIds(attrIds);
    }

}