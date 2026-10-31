package com.seaurl.platform.common.notification;

import com.seaurl.platform.common.model.NotifyRemind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class NotificationDowngradeService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationDowngradeService.class);

    public Mono<Boolean> addNotifyRemind(NotifyRemind notifyRemind) {
        logger.info("deprecated notify side effect ignored, action={}, objectId={}",
                notifyRemind.getSenderAction(), notifyRemind.getObjectId());
        return Mono.just(false);
    }

    public Mono<Void> ignoreDeprecatedNotifySideEffect(String action, String objectId) {
        logger.info("deprecated notify side effect ignored, action={}, objectId={}", action, objectId);
        return Mono.empty();
    }

    @SuppressWarnings("unchecked")
    public Mono<com.seaurl.platform.common.model.NotifyRemind> queryNotifyBy(com.seaurl.platform.common.model.LoginUser loginUser, String objectId, String objectType, String senderAction) {
        logger.info("deprecated queryNotifyBy ignored, objectId={}, objectType={}", objectId, objectType);
        return Mono.empty();
    }
}
