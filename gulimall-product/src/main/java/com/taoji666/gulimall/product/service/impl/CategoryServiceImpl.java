package com.taoji666.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.taoji666.gulimall.product.service.CategoryBrandRelationService;
import com.taoji666.gulimall.product.dao.CategoryDao;
import com.taoji666.gulimall.product.entity.CategoryEntity;
import com.taoji666.gulimall.product.service.CategoryService;
import com.taoji666.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taoji666.common.utils.PageUtils;
import com.taoji666.common.utils.Query;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    //在本项目中，缓存里的都是字符串，因此用StringRedisTemplate就够了，不需要用redis对象的api
    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        //2、组装成父子的树形结构

        //2.1）、找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
             categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildrens(menu,entities));
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());




        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1、检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //比如tree路径是[2,25,225]
    //将所属分类的ID传进来
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        //通过递归找出的是[225 2]，这里将其逆序成[2,25,225]

        Collections.reverse(parentPath);


        return parentPath.toArray(new Long[parentPath.size()]);
    }

    //传入商品的分类属性ID，和一个空list集合
    //最终会依次找出 255 25 2
    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1、递归循环的时候，就收集了当前节点id
        paths.add(catelogId); //
        //category数据表  this是指本service（categoryService）的getById，通过商品分类属性，找到商品本体
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){ //如果该商品的父id不是0，说明还有再上级目录
            findParentPath(byId.getParentCid(),paths);//递归继续查找父id，没有父id了=0的时候，终止
        }
        return paths;

    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    /* @Transactional 开启事务，该方法中的语句只能同时发生，或者同时不发生。
    * 配置类上需要有@EnableTransactionManagement，这里才能开启事务。话说哪个项目不用事务啊
    * 删除redis中的缓存,属性要和@Cacheable中的对应，就是删除@Cacheable 中的value区域的 key 缓存
    * 特别注意：key中的是spel表达式，""里面的还是变量 “‘’”才是普通字符串
    *
    * 为了方便删除，并且不误删除别人的key，因此存储同一类型数据，都可以指定成同一个分区@Cacheable(value = "同一分区"。分区名默认就是缓存前缀
    * */
    //@CacheEvict(value = "category",key = "'getLevel1Categories'") //只能删除一个key,或者所有key，删除多个key的话，用@Caching
    //@CacheEvict(value = "category",allEntries = true) //删除category目录里（分区）的所有key
    @Caching(evict = {  //就是组合使用 缓存的哪几种注解，比如@Cacheable,@CacheEvict,@CachePut,@CacheConfig等等。这里是删除多个key，就组合@CacheEvict即可
            @CacheEvict(value = "category",key = "'getLevel1Categories'"),
            @CacheEvict(value = "category",key = "getCatalogJson")
    })
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);

        //除了更新自己，还要更新关联的数据表。就是在自己的controller使用别人表的service来改关联的有相同字段的表
        //relation表的冗余设计只有两项，因此只更新冗余的两处
        //这个方法mybatis只提供了更新一个数据的，更新两个数据，需要自己重写updateCategory(E x,E y)
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());

        //修改过数据库以后，数据库和缓存中数据已经不一致了，就删除缓存中的数据   相关搜索失效模式
        //实践中，通过springcache的@CacheEvict即可达到以下代码的效果即删除缓存中的数据，完成失效模式
        //redisTemplate.delete("catalogJSON"); //之后业务发现缓存中没有数据，会主动更新缓存
    }

    //indexController 使用的
    //学习的时候，自己写的，实际当然是用框架啦。框架板的本方法 getCatalogJson
    //@Override
    public Map<String, List<Catalog2Vo>> getCatalogJson2() {
        //给缓存中放json字符串，拿出的json字符串，还能逆转化为能用的对象【序列化与反序列化】
        /**
         * 1、空结果缓存：解决缓存穿透
         * 2、设置过期时间（加随机值）：解决缓存雪崩
         *3、加锁：解决缓存击穿
         *
         */
        // 1.从缓存中读取分类信息，缓存中的数据是json字符串（Json跨语言，跨平台兼容）

        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        // 2. 如果缓存中没有数据，再来查询数据库
        //保证数据查询完成后，将数据存放在redis中，这是一个原子操作
        if (StringUtils.isEmpty(catalogJSON)) {
            System.out.println("缓存不命中...将查询数据库");
            //自行编写从数据库查数据的方法getCatalogJsonFromDB()
            Map<String, List<Catalog2Vo>> catalogJsonFromDB = getCatalogJsonFromDB();

            //但是返回的还是查到的 List<Catalog2Vo>>
            return catalogJsonFromDB;
        }
        //缓存中的是Json，因此要转为指定的对象。 Json 转 Map<String, List<Catalog2Vo>>
        return JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
        });
    }
    //本方法是上面getCatalogJson2方法的框架板，并且由于要用框架，直接把getCatalogJsonFromDB()方法展开，并删除与框架重复部分
    //该方法的返回值加入redis，category目录下，加上其他方法上标注@Cacheable(value = "category"的key，已经有多个key了
    //redis中，名字就是db0/category下的 category::getCatalogJson
    @Cacheable(value = "category",key = "#root.methodName")
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        System.out.println("查询了数据库");

        // 性能优化：将数据库的多次查询变为一次。 这里null的意思就是整个数据表一次全部查出来
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        //1、查出所有分类
        //1、1）查出所有一级分类 parentcid字段为0的就是一级分类
        List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);

        //封装数据 将List<CategoryEntity> level1Categories 整理后 封装成Map<String, List<Catalog2Vo>> parentCid。Map的key 就是CatId()
        Map<String, List<Catalog2Vo>> parentCid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //1、每一个的一级分类,查到这个一级分类的二级分类  v.getCatId()=1~20
            List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());

            //2、封装上面的结果
            List<Catalog2Vo> Catalog2Vos = null;
            if (categoryEntities != null) {
                Catalog2Vos = categoryEntities.stream().map(l2 -> {
                    //就是单纯的用有参构造器创建Catalog2Vo对象，注意有内部类List<Category3Vo>
                    Catalog2Vo Catalog2Vo = new Catalog2Vo(v.getCatId().toString(), l2.getCatId().toString(), l2.getName().toString(), null);

                    //1、找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());

                    if (level3Catelog != null) {
                        List<Catalog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                            //2、封装成指定格式
                            Catalog2Vo.Category3Vo category3Vo = new Catalog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                            return category3Vo;
                        }).collect(Collectors.toList());
                        Catalog2Vo.setCatalog3List(category3Vos);
                    }

                    return Catalog2Vo;
                }).collect(Collectors.toList());
            }

            return Catalog2Vos;
        }));

        return parentCid;
    }

    private Map<String, List<Catalog2Vo>> getCatalogJsonFromDB() {
        //本地锁演示，本项目只有分布式锁才能锁住。本地锁，锁不住product微服务集群中的其他product微服务
        //如果只有一个product微服务，没有集群，本地锁就可行
    //  synchronized (this){ //为了配合后面的真方法 getCatalogJsonFromDbWithRedisLock()，只能注掉这个synchronized
            //得到锁以后再次检查redis数据库中是否有数据了，如果没有数据则继续查询
            String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
            //缓存不为null 直接返回
            if (!StringUtils.isEmpty(catalogJSON)) {

                return JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>() {
                });
            }
            System.out.println("查询了数据库");

            // 性能优化：将数据库的多次查询变为一次。 这里null的意思就是整个数据表一次全部查出来
            List<CategoryEntity> selectList = this.baseMapper.selectList(null);

            //1、查出所有分类
            //1、1）查出所有一级分类 parentcid字段为0的就是一级分类
            List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);

            //封装数据 将List<CategoryEntity> level1Categories 整理后 封装成Map<String, List<Catalog2Vo>> parentCid。Map的key 就是CatId()
            Map<String, List<Catalog2Vo>> parentCid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //1、每一个的一级分类,查到这个一级分类的二级分类  v.getCatId()=1~20
                List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());

                //2、封装上面的结果
                List<Catalog2Vo> Catalog2Vos = null;
                if (categoryEntities != null) {
                    Catalog2Vos = categoryEntities.stream().map(l2 -> {
                        //就是单纯的用有参构造器创建Catalog2Vo对象，注意有内部类List<Category3Vo>
                        Catalog2Vo Catalog2Vo = new Catalog2Vo(v.getCatId().toString(), l2.getCatId().toString(), l2.getName().toString(), null);

                        //1、找当前二级分类的三级分类封装成vo
                        List<CategoryEntity> level3Catelog = getParentCid(selectList, l2.getCatId());

                        if (level3Catelog != null) {
                            List<Catalog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                                //2、封装成指定格式
                                Catalog2Vo.Category3Vo category3Vo = new Catalog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                                return category3Vo;
                            }).collect(Collectors.toList());
                            Catalog2Vo.setCatalog3List(category3Vos);
                        }

                        return Catalog2Vo;
                    }).collect(Collectors.toList());
                }

                return Catalog2Vos;
            }));
            // 3. 查询到的数据将对象转成 JSON 存储放到缓存中
            redisTemplate.opsForValue().set("catalogJSON", JSON.toJSONString(parentCid),1, TimeUnit.DAYS);

            return parentCid;

    //    } synchronized 的尾巴，

    }
    /**
     * 1、每一个需要缓存的数据我们都来指定要放到那个名字的缓存。【缓存的分区(按照业务类型分)】,就是在redis下创建一个名为 那个名字的 文件夹
     * 2、@Cacheable 代表当前方法的结果需要缓存，如果缓存中有，方法都不用调用，如果缓存中没有，会调用方法。最后将方法的结果放入缓存
     * 3、默认行为（不符合设计规范，后续需要自定义设计来改）
     *   3.1 如果缓存中有，方法不再调用
     *   3.2 key是默认生成的:缓存的名字::SimpleKey::[](自动生成key值)
     *   3.3 缓存的value值，默认使用jdk序列化机制，将序列化的数据存到redis中（直接进redis看起来像乱码）
     *   3.4 默认时间是 -1：（redis中的过期时间-1 代表永不过期）：不满足缓存设计规范（需要过期时间）
     *
     *   为了满足缓存设计规范，需要 自定义操作 ： key的生成
     *    1. 指定生成缓存的key：key属性指定，接收一个 SpEl表达式
     *    2. 指定缓存的数据的存活时间:spring配置文件中修改存活时间 ttl
     *    3. 将数据保存为json格式: 自定义配置类 MyCacheManager  （方便跨平台，跨语言兼容）
     *         配置方法和原理：见MyCacheConfig.java
     * <p>
     * 4、Spring-Cache的不足之处：
     * 1）、读模式
     * 缓存穿透：查询一个根本不存在的null数据。解决方案：把空数据null也缓存了：配置文件中spring.cache.redis.cache-null-values=true
     * 缓存击穿：大量并发进来同时查询一个正好过期的数据。解决方案：加锁。但是默认是无加锁的;因此使用注解的属性sync = true来解决击穿问题
     * 缓存雪崩：大量的key同时过期。解决：加随机时间。加上过期时间 spring.cache.redis.time-to-live=3600000
     * 2)、写模式：（缓存与数据库一致）
     * 1）、读写加锁。
     * 2）、引入Canal,感知到MySQL的更新去更新Redis
     * 3）、读多写多，直接去数据库查询就行
     * <p>
     * 总结：
     * 常规数据（读多写少，即时性，一致性要求不高的数据，完全可以使用Spring-Cache）：写模式(只要缓存的数据有过期时间就足够了)
     * 特殊数据：特殊设计（就只能使用canal了）
     * <p>
     * 原理：
     * CacheManager(RedisCacheManager)->Cache(RedisCache)->Cache负责缓存的读写
     *
     * @return
     */


    // @Cacheable:将当前方法的返回值保存到缓存中；（如果缓存中有该结果，IndexController再调用该方法，也不会执行）
    //value属性：缓存的分区(按照业务类型分)】,就是在redis下创建一个名为 category 文件夹
    //key属性：就是redis的key，用spel表达式取值，参考官网#root.method.name 代表用方法名getLevel1Categories作为 key
    //sync属性：给微服务加锁，防止多个相同为服务同时进来查询一个正好过期的数据，造成缓存击穿
    //特别注意：key中的是spel表达式，""里面的还是变量  如果写普通字符串，一定要“‘’”
    //找1级分类，1级分类就是pms_category数据表中 cat_level为1 或者 parent_cid为0 的项目就是1级分类
    @Cacheable(value = {"category"}, key = "#root.methodName", sync = true)
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        System.out.println("get Level 1 Categories........");
        long l = System.currentTimeMillis();
        List<CategoryEntity> categoryEntities = this.baseMapper.selectList(
                //这里使用parent_cid=0 来定位一级分类。给parent_cid加了索引后，可以优化性能
                new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        System.out.println("消耗时间：" + (System.currentTimeMillis() - l));
        return categoryEntities;
    }

    /**
     * 从数据库查询并封装数据::分布式锁
     * 明明有Redisson分布式锁框架，但是教学嘛，自己过一下  后面就是用RedissonLock锁框架重写
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedisLock() {

        //1、占分布式锁。去redis占坑。设置过期时间必须和加锁是同步的，保证原子性（避免死锁：突然断电导致锁永远没法删除）
        //占坑api setIfAbsent()。占坑redis源码 SET key value [EX seconds][PX milliseconds][NX][XX]
        String uuid = UUID.randomUUID().toString();//避免误删其他微服务的锁，只删除自己的锁，加一个随机数
        //TimeUnit指时间单位
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) { //lock为true说明加锁成功
            System.out.println("获取分布式锁成功...");
            Map<String, List<Catalog2Vo>> dataFromDb = null;
            try {
                //加锁成功（其他微服务就进不来了）...执行业务
                dataFromDb = getCatalogJsonFromDB(); //这个方法已经取消synchronized了
            } finally {
                // lua 脚本解锁 ，脚本script 从redis官网里面抄过来
                //该脚本锁能确保删除操作的原子性，执行到一半，突然断电也会回滚，不会造成死锁
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                // 删除锁：业务成功以后，要把坑腾出来给别人用
                //传入execute方法的实参，最终会进入上面的脚本中
                redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Collections.singletonList("lock"), uuid);
            }
            //先去redis查询下保证当前的锁是自己的
            //获取值对比，对比成功删除=原子性 lua脚本解锁
            // String lockValue = stringRedisTemplate.opsForValue().get("lock");
            // if (uuid.equals(lockValue)) {
            //     //删除我自己的锁
            //     stringRedisTemplate.delete("lock");
            // }
            return dataFromDb;
        } else {
            System.out.println("获取分布式锁失败...等待重试...");
            //加锁失败，说明有其他微服务在给数据库写数据，我们只能等待...重试机制
            //休眠一百毫秒
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDbWithRedisLock();     //自旋的方式
        }
    }

    /**
     * 用RedissonLock重写上述业务
     * 缓存里的数据  如何和  数据库的数据  保持一致？？
     * 缓存数据一致性
     * 1)、双写模式：数据库改完，就跟着改缓存   （本身很麻烦，还要处理脏数据问题（加锁），整体效率太低，不好）
     * 2)、失效模式：数据库改完，直接把缓存删了  下一次进行主动查询的时候，发现缓存中没数据，再去去数据库查数据
     * 两个方式都会产生脏数据问题（数据不一致问题），脏数据问题都是多线程来执行代码导致，因此都可以通过加锁解决，但是加锁会影响性能
     * 因此，经常改动的数据，就不用缓存了。
     *
     * 大绝招：Canal框架：伪装成mysql的从机，发现mysql变化，自动同步后推送给redis
     *
     * 本项目中的数据一致性解决方案，就是设置过期时间+ 读写锁 + 失效模式（最终用springcache的@CacheEvict来做失效模式）：
     * 1、缓存中的所有数据都有过期时间，数据过期，下一次触发主动更新
     * 2、读写数据的时候，加上分布式的读写锁。（经常读，偶尔写的特点，就是读写锁）
     * @return
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        //1、占分布式锁。去redis占坑
        //（锁的粒度，越细越快:具体体现在锁的名字越具体，越没人用（不会误锁其他微服务，也不会被其他微服务误锁），因此效率越快
        // 为了避免锁名重复，锁命名规则为 比如 11号商品： product-11-lock
        //RLock catalogJsonLock = redissonClient.getLock("catalogJson-lock");

        //创建读锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("catalogJson-lock");
        //加锁
        RLock rLock = readWriteLock.readLock();

        Map<String, List<Catalog2Vo>> dataFromDb = null;

        //加锁以后，在代码块中写业务
        try {
            rLock.lock();
            //加锁成功...执行业务
            dataFromDb = getCatalogJsonFromDB();
        } finally {
            rLock.unlock(); //释放锁。之后，其他线程才有机会拿锁执行代码
        }
        return dataFromDb;
    }


    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root,List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1、找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity,all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            //2、菜单的排序
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }
    //单独抽取的方法，查出所有parent_id为规定值的行
    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parentCid) {
        return selectList.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }



}