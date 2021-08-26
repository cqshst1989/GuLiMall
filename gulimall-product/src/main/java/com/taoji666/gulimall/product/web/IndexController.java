package com.taoji666.gulimall.product.web;

import com.taoji666.gulimall.product.entity.CategoryEntity;
import com.taoji666.gulimall.product.service.CategoryService;
import com.taoji666.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/*
Web包下的controller 都是后台渲染视图的，没有前后端分离的controller

 * @RestController = @Controller + @ResponseBody
 * @ReponseBody 处理返回Json数据.前后端分离的项目，肯定要。 但是后端渲染的thymeleaf做的，就不需要这个注解
 * 这里返回视图
 * 默认前缀（如果不去application.yml配置的话）：classpath:/templates/
 * 默认后缀：.html
 */
@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    private RedissonClient redisson; //测试redisson分布锁

    @Autowired
    private StringRedisTemplate stringRedisTemplate;



    //当从浏览器访问/ 或者 /index.html的时候，都去主页
    @GetMapping(value = {"/", "index.html"})
    private String indexPage(Model model) {
        //1、查出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categories();

        //将查到的结果放到http域中
        model.addAttribute("categories", categoryEntities);

        //视图解析器进行拼串，结果是classpath:/templates/index.html
        return "index";
    }

    /**
     * 二级、三级分类数据
     *处理前端 catalogLoader.js 用jquery发的ajax请求
     * @return
     */
    @GetMapping(value = "/index/catalog.json")
    @ResponseBody //返回json数据，而不是返回页面，因此需要用@ResponseBody来将return的map转换成json
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        //找出分类数据，封装成map后，返回给catalogloader.js
        return categoryService.getCatalogJson();
    }

    @ResponseBody
    @GetMapping(value = "/hello")
    public String hello() {

        //1、获取一把锁，只要锁的名字一样，就是同一把锁
        RLock myLock = redisson.getLock("my-lock");

        //2、加锁（默认加的锁都是30s） 一旦加了锁，就只能一个人访问/hello了，除非释放掉
          //1）、锁的自动续期，如果业务超长，运行期间自动锁上新的30s。不用担心业务时间长，锁自动过期被删掉
          //2）、加锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认会在30s内自动过期，不会产生死锁问题
        myLock.lock();//阻塞式等待：一直等到拿到锁为止，然后加锁。拿不到锁，就一直等


        //该带参方法，可以设置10秒钟自动解锁,但是在锁时间到了以后，不会自动续期设置过期时间，因此自动解锁时间一定要大于业务执行时间
        // myLock.lock(10,TimeUnit.SECONDS);

        //lock([....])方法总结
        //1、如果我们传递了锁的超时时间(带参)，就发送给redis执行脚本，进行占锁，默认超时就是 我们制定的时间
        //2、如果我们未指定锁的超时时间（无参），就使用 lockWatchdogTimeout = 30 * 1000 【看门狗默认时间】
            //只要占锁成功，就会启动一个定时任务【重新给锁设置过期时间，新的过期时间就是看门狗的默认时间】,每隔10秒都会自动的再次续期，续成30秒
        // internalLockLeaseTime  根据源码，续期时间= 看门狗时间（10s） / 3， 10s


        //推荐使用带参的 myLock.lock(30,TimeUnit.SECONDS);因为省掉了续期操作，设置到30s就好，到期自动解锁，没到期手动解锁。总体而言性能更好


        try {
            System.out.println("加锁成功，执行业务..." + Thread.currentThread().getId());
            try { TimeUnit.SECONDS.sleep(20); } catch (InterruptedException e) { e.printStackTrace(); }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            //3、解锁  假设解锁代码没有运行，Redisson会不会出现死锁
            System.out.println("释放锁..." + Thread.currentThread().getId());
            myLock.unlock();
        }

        return "hello";
    }

  /*  writeValue() 和 readValue()方法 演示读写锁
    为了达成以下效果，读写锁是一把锁“rw-lock"
    读写锁：确保正在更新数据的时候，读不到旧数据，一直等待直到数据更新完成。 有点像mysql的禁止不可重复读
    读写锁最终效果：一定能读到最新数据

    写锁：排它锁（互斥锁，独享锁），不准其他线程同时进来修改数据，只要写锁没释放，其他线程就必须一直等待
    读锁：共享锁，其他线程可以一起读，但是不准修改

   一个线程在调用读方法readValue()，同时另一个线程也在调用读方法readValue()：web浏览器中，两个页面同时访问xxx/read
    读 + 读 ：相当于无锁，并发读，只会在Redis中记录好，所有当前的读锁。他们都会同时加锁成功

    同理 一个线程在调用写方法writeValue()，同时另一个线程调用读方法readValue()：web浏览器中，一个页面访问xxx/write，另一个访问xxx/read
    写 + 读 ：必须等待写锁释放

    写 + 写 ：阻塞方式
    读 + 写 ：有读锁。写也需要等待

     规律：只要有写的存在，就必须等待
    */

    @GetMapping(value = "/write")
    @ResponseBody
    public String writeValue() {
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        String s = "";
        RLock rLock = readWriteLock.writeLock();
        try {
            //1、改数据加写锁
            rLock.lock();
            s = UUID.randomUUID().toString();
            ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
            ops.set("writeValue",s); //给redis存入数据
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            rLock.unlock(); //释放写锁，然后readValue()方法，才可以操作(读写方法用的同一把锁)
        }

        return s;
    }

    @GetMapping(value = "/read")
    @ResponseBody
    public String readValue() {
        String s = "";
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("rw-lock");
        //加读锁，拿到锁加锁成功后再执行业务代码
        RLock rLock = readWriteLock.readLock();
        try {
            rLock.lock();
            ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
            s = ops.get("writeValue");
            try { TimeUnit.SECONDS.sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
        }

        return s;
    }


    /**
     * 演示 分布式闭锁 GetCountDownLatch
     * 业务场景：放假了、保安等所有班级全部走完了，再锁门5个班，全部走完，我们才可以锁大门
     * 保安业务：/lockDoor  lockDoor()方法：
     * {id}班 走人业务：/gogogo/{id}     id代表1班，2班，3班
     *
     * 保安业务 和 走人业务  通过分布式锁 "door"产生关系
     *
     * 分布式锁的写法和 单体应用锁 写法都一样，就是用于分布式服务而已
     */

    @GetMapping(value = "/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {

        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5); //设置总共5个班，gogogo业务每次被访问，可以走一个班
        door.await();       //等待闭锁完成

        return "放假了...";
    }
    //班级走人业务， id 班 走人，一共走5个班，lockDoor业务就可以继续执行了
    @GetMapping(value = "/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();       //计数-1，总计数  上面door.trySetCount(5)设置好了是5

        return id + "班的人都走了...";
    }

    /**
     * 信号量（Semaphore）
     *
     * 业务场景：车库停车，一共3个车位，假设已经停满，走一辆车（go方法），车位+1 ，停一辆车（park方法），车位-1
     * 没车位的时候，就一直等待
     * 车位值（即信号量），直接在redis里面设置一个键值对就好。 key为park  value 为 3 （假装3个车位）
     *
     * 信号量也可以做分布式限流
     */

    //代表停车，停车一次，车位-1（即信号量-1）
    @GetMapping(value = "/park")
    @ResponseBody
    public String park() throws InterruptedException {

        RSemaphore park = redisson.getSemaphore("park");
        park.acquire();     //获取一个信号、即获取一个值=占一个车位   车位-1（这里车位就是信号）

        //tryAcquire 有车位就停，没有车位就直接返回false，不用阻塞式等待车位来。acquire就会一直阻塞式等待车位来
        boolean flag = park.tryAcquire();

        if (flag) {
            //执行业务
        } else {
            return "error";
        }

        return "ok=>" + flag;
    }
    //代表汽车走掉，执行一次此方法  车位+1
    @GetMapping(value = "/go")
    @ResponseBody
    public String go() {
        RSemaphore park = redisson.getSemaphore("park");
        park.release();     //释放一个车位
        return "ok";
    }



}
