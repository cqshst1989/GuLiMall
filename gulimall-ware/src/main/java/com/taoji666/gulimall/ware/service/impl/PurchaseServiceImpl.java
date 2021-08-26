package com.taoji666.gulimall.ware.service.impl;

import com.taoji666.common.constant.WareConstant;
import com.taoji666.gulimall.ware.service.WareSkuService;
import com.taoji666.gulimall.ware.vo.MergeVo;
import com.taoji666.gulimall.ware.entity.PurchaseDetailEntity;
import com.taoji666.gulimall.ware.service.PurchaseDetailService;
import com.taoji666.gulimall.ware.vo.PurchaseDoneVo;
import com.taoji666.gulimall.ware.vo.PurchaseItemDoneVo;
import com.taoji666.gulimall.ware.dao.PurchaseDao;
import com.taoji666.gulimall.ware.entity.PurchaseEntity;
import com.taoji666.gulimall.ware.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService detailService;

    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),//分页条件和原来一样
                //0 和 1 状态的采购单都是没有被执行的，因此就查这两个采购单
                new QueryWrapper<PurchaseEntity>().eq("status",0).or().eq("status",1)
        );

        return new PageUtils(page);
    }

  /*
   {
        purchaseId: 1, //整单id  采购单
        items:[1,2,3,4] //合并项集合  将采购详情单1、2、3、4 合并成一个采购单ID
    }
* 专门为这两个参数写一个 MergeVo 来收集前端数据


*/

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();//1
        if(purchaseId == null){
            //1、新建一个采购单
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            //新建一个WareConstant枚举 4个对象（CREATED（0 新建），ASSIGNED（1 已分配），RECEIVE（2 已领取），FINISH（3 已完成），HASERROR（4 有异常））
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());


            purchaseEntity.setCreateTime(new Date());//设置采购单的创建日期
            purchaseEntity.setUpdateTime(new Date());//设置采购单的更新日期
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }

        //TODO 确认采购单状态是0,1才可以合并
        //前端传来的 1~4 共计4个采购单，要合并成一个采购单
        List<Long> items = mergeVo.getItems();//前端传入的将要进行合并的采购单 id 1~4 采购单要合并
        Long finalPurchaseId = purchaseId; //这些采购单 要 合并的 采购需求表 id

        List<PurchaseDetailEntity> collect = items.stream().map(i -> {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();

            detailEntity.setId(i); //合并第i个采购单的详情  前端传来的从1 ~ 4
            detailEntity.setPurchaseId(finalPurchaseId); //4个采购单的详情，共用一个采购id了
            detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());//4个采购单详情状态都变成已分配
            return detailEntity;
        }).collect(Collectors.toList());


        detailService.updateBatchById(collect);//批量更新采购详情，所有采购单的采购详情都加入采购详情表，并且ID都统一，所以合并

        //已完成采购的采购单更新
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);  //已完成采购的采购单
        purchaseEntity.setUpdateTime(new Date()); //修改更新日期
        this.updateById(purchaseEntity);
    }

    /**
     *
     * @param ids 采购单id
     *  领取采购单： 点击领取以后，采购单 和 采购detail 数据表 响应的状态发生改变

     * 前端请求： POST   /ware/purchase/received
     * 前端请求
     *  [1,2,3,4]//前端传来的采购单id（即将领取的）
     */
    @Override
    public void received(List<Long> ids) {
        //1、确认当前采购单是新建或者已分配状态
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter(item -> { //过滤 采购单状态，只要是新建 或者 已分配 的
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                    item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map(item->{  //过滤完成以后，再来统一改变采购单状态  “已领取”
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());

        //2、改变采购单的状态  保存数据库
        this.updateBatchById(collect);



        //3、改变采购项的状态（明细表里面的状态）
        collect.forEach((item)->{   //通过采购单的id ，定位的 detail表里面的采购项
            List<PurchaseDetailEntity> entities = detailService.listDetailByPurchaseId(item.getId());
            List<PurchaseDetailEntity> detailEntities = entities.stream().map(entity -> {
                PurchaseDetailEntity entity1 = new PurchaseDetailEntity();
                entity1.setId(entity.getId());
                entity1.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return entity1;
            }).collect(Collectors.toList());
            detailService.updateBatchById(detailEntities);
        });
    }


 /* 库存系统07 完成采购
POST   /ware/purchase/done
{
id: 123,   //采购单id
items: [{itemId:1,status:4,reason:""}]  // 哪些项目已经完成采购，没有完成status4，原因是什么
}
*/
    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {

        Long id = doneVo.getId();


        //2、改变采购项的状态  改采购entity数据表
        Boolean flag = true;

        List<PurchaseItemDoneVo> items = doneVo.getItems(); //已完成的采购单ID，将要将其状态改变

        List<PurchaseDetailEntity> updates = new ArrayList<>(); //只修改 采购单detail数据表的itemid，status，reason

        for (PurchaseItemDoneVo item : items) { //遍历前端传来的采购单项，就是采购员在app上点击（未）完成采购的项目

            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();//就是动这个表的相关数据

            //如果前端传来的是采购失败HASEROR
            if(item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()){
                flag = false;
                detailEntity.setStatus(item.getStatus());//设置数据库中 采购失败
            }else{//3、采购成功
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());//先设置改变状态字段
                //将成功采购的进行入库  改库存数据表
                PurchaseDetailEntity entity = detailService.getById(item.getItemId());
                wareSkuService.addStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum());

            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }

        detailService.updateBatchById(updates);

        //改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        //flag专门用来判断采购成功还是失败， 推断出这里的状态是不是顺利完成采购
        purchaseEntity.setStatus(flag?WareConstant.PurchaseStatusEnum.FINISH.getCode():WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);




    }

}