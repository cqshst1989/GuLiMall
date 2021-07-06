package com.taoji666.gulimall.gulimallproduct.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import com.taoji666.gulimall.gulimallproduct.dao.CategoryDao;
import com.taoji666.gulimall.gulimallproduct.entity.CategoryEntity;
import com.taoji666.gulimall.gulimallproduct.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }
    //用于查出所有分类
    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2、组装成父子的树形结构

        //2.1）、找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((menu)->{ //children属性是自己加入实体类的属性，menu是CategoryEntity的形参
            menu.setChildren(getChildrens(menu,entities)); //调用自己编写的getChildrens()，传入实参menu（menu在这个函数时实参） 和 entities
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList()); //有可能空指针异常，所以要给为空的处理方法
        return level1Menus;

    }

    //递归查找所有菜单的子菜单。传入的第一个参数是一级分类level1，第二个参数是所有菜单。
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            //从所有菜单中找到level1 的下级
            return categoryEntity.getParentCid() == root.getCatId(); //没有下级了 判定为false，递归终止
        }).map(categoryEntity -> {
            //map()将一个流转换为另一个流，这里转换为children 其实 children就是categoryentity的子条目
            categoryEntity.setChildren(getChildrens(categoryEntity,all)); //调用自己getchildrens()，递归，直到没有下级了
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            //2、菜单的排序
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort()); //有可能空指针异常，所以要给为空的处理方法
        }).collect(Collectors.toList());

        return children;
    }



}