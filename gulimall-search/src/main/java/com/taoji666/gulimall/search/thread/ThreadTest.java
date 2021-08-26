package com.taoji666.gulimall.search.thread;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author: TaoJi
 * @date: 2021/8/14 14:43
 *
 * 测试多线程，不能在test中测试，没得多线程效果。只能在main中测试
 */
public class ThreadTest {


    //系统中线程池只要一两个，每个异步任务，提交给线程池，让他自己执行。
    //快速创建一个线程池，配置10个线程
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // System.out.println("main......start.....");
        // Thread thread = new Thread01();
        // thread.start();
        // System.out.println("main......end.....");

        // Runable01 runable01 = new Runable01();
        // new Thread(runable01).start();

        // FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
        // new Thread(futureTask).start();
        // System.out.println(futureTask.get());

        // service.execute(new Runable01());
        // Future<Integer> submit = service.submit(new Callable01());
        // submit.get();

        System.out.println("main......start.....");
        // CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        //     System.out.println("当前线程：" + Thread.currentThread().getId());
        //     int i = 10 / 2;
        //     System.out.println("运行结果：" + i);
        // }, executor);

        /**
         * 方法完成后的处理
         */
        // CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
        //     System.out.println("当前线程：" + Thread.currentThread().getId());
        //     int i = 10 / 0;
        //     System.out.println("运行结果：" + i);
        //     return i;
        // }, executor).whenComplete((res,exception) -> {
        //     //虽然能得到异常信息，但是没法修改返回数据
        //     System.out.println("异步任务成功完成了...结果是：" + res + "异常是：" + exception);
        // }).exceptionally(throwable -> {
        //     //可以感知异常，同时返回默认值
        //     return 10;
        // });

        /**
         * 方法执行完后端处理
         */
        // CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
        //     System.out.println("当前线程：" + Thread.currentThread().getId());
        //     int i = 10 / 2;
        //     System.out.println("运行结果：" + i);
        //     return i;
        // }, executor).handle((result,thr) -> {
        //     if (result != null) {
        //         return result * 2;
        //     }
        //     if (thr != null) {
        //         System.out.println("异步任务成功完成了...结果是：" + result + "异常是：" + thr);
        //         return 0;
        //     }
        //     return 0;
        // });

    }
}