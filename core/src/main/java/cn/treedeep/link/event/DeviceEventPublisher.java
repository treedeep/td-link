package cn.treedeep.link.event;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>事件发布服务</p>
 *
 * @author 周广明
 * @since 2025/3/30 09:00
 */
@Slf4j
public class DeviceEventPublisher {
    // 使用CopyOnWriteArrayList保证线程安全，适合读多写少的场景
    private final List<Consumer<DeviceEvent>> subscribers = new CopyOnWriteArrayList<>();

    // 使用ConcurrentMap存储特定类型的事件处理器
    private final ConcurrentMap<Class<?>, List<Consumer<?>>> typedSubscribers = new ConcurrentHashMap<>();

    /**
     * 订阅所有事件
     */
    public void subscribe(Consumer<DeviceEvent> subscriber) {
        log.info("订阅所有事件");
        subscribers.add(subscriber);
    }

    /**
     * 订阅特定类型事件
     */
    public <T extends DeviceEvent> void subscribe(Class<T> eventType, Consumer<T> subscriber) {
        log.info("订阅【{}】类型事件", eventType.getSimpleName());
        typedSubscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(subscriber);
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(Consumer<DeviceEvent> subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * 取消订阅特定类型事件
     */
    public <T extends DeviceEvent> void unsubscribe(Class<T> eventType, Consumer<T> subscriber) {
        List<Consumer<?>> handlers = typedSubscribers.get(eventType);
        if (handlers != null) {
            handlers.remove(subscriber);
        }
    }

    /**
     * 发布事件
     */
    public void publishEvent(DeviceEvent event) {
        // 处理通用订阅者
        subscribers.forEach(subscriber -> {
            try {
                subscriber.accept(event);
            } catch (Exception e) {
                // 处理异常但不中断其他订阅者
                System.err.println("事件处理异常: " + e.getMessage());
            }
        });

        // 处理特定类型订阅者
        List<Consumer<?>> handlers = typedSubscribers.get(event.getClass());
        if (handlers != null) {
            handlers.forEach(handler -> {
                try {
                    @SuppressWarnings("unchecked")
                    Consumer<DeviceEvent> castHandler = (Consumer<DeviceEvent>) handler;
                    castHandler.accept(event);
                } catch (Exception e) {
                    System.err.println("特定类型事件处理异常: " + e.getMessage());
                }
            });
        }
    }
}