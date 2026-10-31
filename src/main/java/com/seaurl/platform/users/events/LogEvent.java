//package com.seaurl.platform.users.events;
//
//import com.alibaba.fastjson.JSONObject;
//import com.seaurl.platform.common.model.UserEvent;
//import com.seaurl.platform.users.model.UserPoints;
//import com.seaurl.platform.users.model.UserPointsHistory;
//import com.seaurl.platform.users.request.UserPointsRequest;
//import com.seaurl.platform.users.service.UserPointsHistoryService;
//import com.seaurl.platform.users.service.UserPointsService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.amqp.rabbit.annotation.Queue;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.messaging.Message;
//import org.springframework.stereotype.Component;
//import org.springframework.util.StringUtils;
//
//import java.util.Date;
//import java.util.function.Consumer;
//
//@Component
//public class LogEvent {
//    private Logger log = LoggerFactory.getLogger(this.getClass());
//
//    @Bean
//    public Consumer<Message<String>> logConsumer() {
//        return message -> {
//            System.out.println("Success Rescive message from logConsumer: " + message.getPayload());
//        };
//    }
//
//}
