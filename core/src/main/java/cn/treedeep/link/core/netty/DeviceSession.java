package cn.treedeep.link.core.netty;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceSession {

    public DeviceSession(int deviceId) {
        this.deviceId = deviceId;
        this.createTime = LocalDateTime.now();
        this.status = SessionStatus.INACTIVE;
    }

    private int deviceId;
    private short sessionId;
    private int taskId;
    private LocalDateTime createTime;
    private LocalDateTime lastActiveTime;
    private int frameSequence;
    private SessionStatus status;

    public boolean isExpired(int sessionTimeoutMinutes) {
        if (status == SessionStatus.CLOSED) {
            return true;
        }
        
        if (lastActiveTime == null) {
            lastActiveTime = createTime;
        }
        
        return lastActiveTime.plusMinutes(sessionTimeoutMinutes).isBefore(LocalDateTime.now());
    }


    public enum SessionStatus {
        INACTIVE, ACTIVE, CLOSED
    }
}