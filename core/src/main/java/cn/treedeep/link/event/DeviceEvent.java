package cn.treedeep.link.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>事件模型</p>
 *
 * @author 周广明
 * @since 2025/3/30 09:00
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEvent {
    private String eventType;
    private int deviceId;
    private int taskId;
    private Object data;
    private LocalDateTime timestamp = LocalDateTime.now();

    public DeviceEvent(String eventType, int deviceId, Object data) {
        this.eventType = eventType;
        this.deviceId = deviceId;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public DeviceEvent(String eventType, int deviceId, int taskId, Object data) {
        this.eventType = eventType;
        this.deviceId = deviceId;
        this.taskId = taskId;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
}