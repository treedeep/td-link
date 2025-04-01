package cn.treedeep.link.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {
    private int deviceId;
    private short sessionId;
    private String remoteAddress;
    private LocalDateTime connectedTime;
    private LocalDateTime lastActiveTime;
    private int taskId;
    private String status;
}