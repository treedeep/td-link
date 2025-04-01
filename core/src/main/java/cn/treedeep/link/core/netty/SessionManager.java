package cn.treedeep.link.core.netty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class SessionManager {

    private final ConcurrentMap<Integer, DeviceSession> sessions = new ConcurrentHashMap<>();
    private final AtomicInteger sessionIdCounter = new AtomicInteger(1);

    /**
     * 获取设备会话
     *
     * @param deviceId 设备ID
     * @return 设备会话，如果不存在则返回null
     */
    public DeviceSession getSession(int deviceId) {
        return sessions.get(deviceId);
    }

    /**
     * 获取会话ID
     *
     * @param deviceId 设备ID
     * @return 会话ID，如果会话不存在则返回null
     */
    public Short getSessionId(int deviceId) {
        DeviceSession session = sessions.get(deviceId);
        return session == null ? null : session.getSessionId();
    }

    /**
     * 移除设备会话
     *
     * @param deviceId 设备ID
     */
    public DeviceSession removeSession(int deviceId) {
        DeviceSession removed = sessions.remove(deviceId);
        if (removed != null) {
            log.info("设备会话已移除: {}", removed.getSessionId());
        }
        return removed;
    }

    /**
     * 创建设备会话
     *
     * @param deviceId 设备ID
     * @return 新创建的设备会话
     */
    public DeviceSession createSession(int deviceId) {
        // 检查会话是否已存在
        DeviceSession existingSession = sessions.get(deviceId);
        if (existingSession != null) {
            log.warn("设备会话已存在，将被替换: {}", deviceId);
            sessions.remove(deviceId);
        }

        short sessionId = generateSessionId(deviceId);
        DeviceSession session = new DeviceSession(deviceId);
        session.setSessionId(sessionId);
        session.setLastActiveTime(LocalDateTime.now());

        sessions.put(deviceId, session);
        log.debug("设备会话已创建: {} -> sessionId: {}", deviceId, sessionId);
        return session;
    }

    /**
     * 更新设备最后活动时间
     *
     * @param heartbeat 设备心跳包
     */
    public void updateLastActive(int deviceId, int taskId) {
        DeviceSession session = sessions.get(deviceId);

        if (session != null) {
            session.setLastActiveTime(LocalDateTime.now());
            session.setTaskId(taskId);
            session.setStatus(DeviceSession.SessionStatus.ACTIVE);
        }
    }

    /**
     * 获取所有活跃会话
     *
     * @return 会话列表
     */
    public List<DeviceSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    /**
     * 获取活跃会话数量
     *
     * @return 会话数量
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * 生成会话ID
     *
     * @param deviceId 设备ID
     * @return 会话ID
     */
    private short generateSessionId(int deviceId) {
        // 使用设备ID和计数器生成唯一会话ID
        short sessionId = (short) ((deviceId ^ sessionIdCounter.getAndIncrement()) & 0xFFFF);
        log.debug("生成会话ID: {} -> {}", deviceId, sessionId);
        return sessionId;
    }

}