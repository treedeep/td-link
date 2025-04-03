package cn.treedeep.link.event;

import cn.treedeep.link.service.SseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DeviceEventListener {
    private static final Logger log = LoggerFactory.getLogger(DeviceEventListener.class);

    @Autowired
    public DeviceEventListener(DeviceEventPublisher eventPublisher, SseService sseService) {

        // 订阅事件
        eventPublisher.subscribe(event -> {
            log.debug("接收到设备事件: {}", event);
            sseService.sendEventToAll(event);
            // sseService.sendEventToClient("DEVICE:" + event.getDeviceId(), event);
        });
    }


}