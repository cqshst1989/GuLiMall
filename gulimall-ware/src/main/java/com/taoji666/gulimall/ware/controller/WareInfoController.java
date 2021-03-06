package com.taoji666.gulimall.ware.controller;

import java.util.Arrays;
import java.util.Map;

import com.taoji666.gulimall.ware.entity.WareInfoEntity;
import com.taoji666.gulimall.ware.service.WareInfoService;
import com.taoji666.gulimall.ware.vo.FareVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * 仓库信息
 *
 * @author TaoJi
 *
 *
 */
@RestController
@RequestMapping("ware/wareinfo")
public class WareInfoController {
    @Autowired
    private WareInfoService wareInfoService;

    /**开发文档  库存系统 01  仓库列表
     * 请求仓储列表，前端请求  GET  /ware/wareinfo/list
     * {
     *    page: 1,            //当前页码
     *    limit: 10,          //每页记录数
     *    sidx: 'id',         //排序字段
     *    order: 'asc/desc',  //排序方式
     *    key: '华为'         //检索关键字
     * }
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:wareinfo:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareInfoService.queryPage(params);

        return R.ok().put("page", page);
    }



    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:wareinfo:info")
    public R info(@PathVariable("id") Long id){
		WareInfoEntity wareInfo = wareInfoService.getById(id);

        return R.ok().put("wareInfo", wareInfo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:wareinfo:save")
    public R save(@RequestBody WareInfoEntity wareInfo){
		wareInfoService.save(wareInfo);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:wareinfo:update")
    public R update(@RequestBody WareInfoEntity wareInfo){
		wareInfoService.updateById(wareInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:wareinfo:delete")
    public R delete(@RequestBody Long[] ids){
		wareInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }
    /**
    *  根据选择的收货地址 获取运费
     *
     * */
    @RequestMapping("/fare/{addrId}") //相当于@RequestMapping(method = RequestMethod.GET)=@GetMapping
    public FareVo getFare(@PathVariable("addrId") Long addrId) {
        return wareInfoService.getFare(addrId);
    }

}
