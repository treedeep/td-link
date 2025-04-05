package cn.treedeep.link.task;

import cn.treedeep.link.config.LinkConfig;
import cn.treedeep.link.event.DefaultDeviceEvent;
import cn.treedeep.link.event.DeviceEvent;
import cn.treedeep.link.event.DeviceEventPublisher;
import cn.treedeep.link.netty.ChannelManager;
import cn.treedeep.link.netty.DeviceSession;
import cn.treedeep.link.netty.SessionManager;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话清理任务
 * 定期检查并清理过期的设备会话
 */
@Slf4j
public class SessionCleanupTask {

    private final LinkConfig config;
    private final SessionManager sessionManager;
    private final ChannelManager channelManager;
    private final DeviceEventPublisher eventPublisher;

    public SessionCleanupTask(LinkConfig config,
                              SessionManager sessionManager,
                              ChannelManager channelManager,
                              DeviceEventPublisher eventPublisher) {
        this.config = config;
        this.sessionManager = sessionManager;
        this.channelManager = channelManager;
        this.eventPublisher = eventPublisher;
    }

    public void cleanupExpiredSessions() {
        log.info("开始清理过期会话...");
        List<DeviceSession> sessions = sessionManager.getAllSessions();
        int expiredCount = 0;

        for (DeviceSession session : sessions) {
            if (session.isExpired(config.getSessionTimeoutMinutes())) {
                int deviceId = session.getDeviceId();
                log.info("会话过期，设备ID: {}, 最后活动时间: {}", deviceId, session.getLastActiveTime());

                // 关闭通道
                Channel channel = channelManager.getChannel(deviceId);
                if (channel != null && channel.isActive()) {
                    channel.close();
                }

                // 移除会话
                sessionManager.removeSession(deviceId);
                channelManager.removeChannel(deviceId);

                // 发布会话过期事件
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("reason", "SESSION_TIMEOUT");
                eventData.put("lastActiveTime", session.getLastActiveTime().toString());

                DeviceEvent event = new DefaultDeviceEvent(
                        "SESSION_EXPIRED",
                        deviceId,
                        eventData
                );
                eventPublisher.publishEvent(event);

                expiredCount++;
            }
        }

        log.info("会话清理完成，共清理 {} 个过期会话", expiredCount);
    }
}