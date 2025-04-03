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

public interface DeviceEvent {
    String getType();

    int getDeviceId();

    int getTaskId();

    Object getData();

    LocalDateTime getTimestamp();
}