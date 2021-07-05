package com.taoji666.gulimall.gulimallware.controller;

import java.util.Arrays;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.taoji666.gulimall.gulimallware.entity.UndoLogEntity;
import com.taoji666.gulimall.gulimallware.service.UndoLogService;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.R;



/**
 * 
 *
 * @author taoji
 * @email 290691048@qq.com
 * @date 2021-07-04 11:50:55
 */
@RestController
@RequestMapping("gulimallware/undolog")
public class UndoLogController {
    @Autowired
    private UndoLogService undoLogService;

    /**
     * 列表
     */
    @RequestMapping("/list")
   // @RequiresPermissions("gulimallware:undolog:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = undoLogService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("gulimallware:undolog:info")
    public R info(@PathVariable("id") Long id){
		UndoLogEntity undoLog = undoLogService.getById(id);

        return R.ok().put("undoLog", undoLog);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
   // @RequiresPermissions("gulimallware:undolog:save")
    public R save(@RequestBody UndoLogEntity undoLog){
		undoLogService.save(undoLog);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
   // @RequiresPermissions("gulimallware:undolog:update")
    public R update(@RequestBody UndoLogEntity undoLog){
		undoLogService.updateById(undoLog);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
   // @RequiresPermissions("gulimallware:undolog:delete")
    public R delete(@RequestBody Long[] ids){
		undoLogService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
