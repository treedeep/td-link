package cn.treedeep.link.event;

import cn.treedeep.link.core.event.DeviceEvent;
import cn.treedeep.link.service.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DeviceEventListener {

    private final SseService sseService;

    @Autowired
    public DeviceEventListener(SseService sseService) {
        this.sseService = sseService;
    }

    @EventListener
    @Async
    public void handleDeviceEvent(DeviceEvent event) {
        log.debug("接收到设备事件: {}", event);
        sseService.sendEventToAll(event);
        // sseService.sendEventToClient("DEVICE:" + event.getDeviceId(), event);
    }
}