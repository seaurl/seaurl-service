//package com.seaurl.platform.space.handler;
//
//import java.util.concurrent.Flow;
//
//public class SSESubscriber implements Flow.Subscriber<String> {
//    private Flow.Subscription subscription;
//
//    @Override
//    public void onSubscribe(Flow.Subscription subscription) {
//        this.subscription = subscription;
//        subscription.request(1);
//    }
//
//    @Override
//    public void onNext(String item) {
//        // 处理每一行 SSE 数据
//        System.out.println("Received: " + item);
//        // 请求下一个事件
//        subscription.request(1);
//    }
//
//    @Override
//    public void onError(Throwable throwable) {
//        throwable.printStackTrace();
//    }
//
//    @Override
//    public void onComplete() {
//        System.out.println("Stream completed");
//    }
//}
