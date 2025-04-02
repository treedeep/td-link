package cn.treedeep.link.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>事件发布服务</p>
 *
 * @author 周广明
 * @since 2025/3/30 09:00
 */
@Service
public class DeviceEventPublisher implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    public void publishEvent(DeviceEvent event) {
        publisher.publishEvent(event);
    }
}