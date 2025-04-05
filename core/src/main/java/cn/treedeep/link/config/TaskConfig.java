package cn.treedeep.link.config;

import cn.treedeep.link.event.DeviceEventPublisher;
import cn.treedeep.link.netty.ChannelManager;
import cn.treedeep.link.netty.ServerHeartbeat;
import cn.treedeep.link.netty.SessionManager;
import cn.treedeep.link.task.HeartbeatTask;
import cn.treedeep.link.task.SessionCleanupTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Duration;

@Slf4j
@Configuration("td_link_TaskConfig")
public class TaskConfig {

    @Bean
    @ConditionalOnProperty(name = "link.cleanupSessions", havingValue = "true")
    public ScheduledTaskRegistrar sessionCleanupTask(LinkConfig config,
                                                     SessionManager sessionManager,
                                                     ChannelManager channelManager,
                                                     DeviceEventPublisher eventPublisher) {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        registrar.addFixedRateTask(
                () -> new SessionCleanupTask(config, sessionManager, channelManager, eventPublisher)
                        .cleanupExpiredSessions(), Duration.ofMinutes(config.getCleanupExpiredSessions())
        );
        return registrar;
    }

    @Bean
    @ConditionalOnProperty(name = "link.serverHeartbeat", havingValue = "true")
    public ScheduledTaskRegistrar heartbeatTask(LinkConfig config, ChannelManager channelManager, ServerHeartbeat serverHeartbeat) {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
        registrar.addFixedRateTask(
                () -> new HeartbeatTask(channelManager).scheduleHeartbeatInterval(serverHeartbeat),
                Duration.ofSeconds(config.getHeartbeatInterval())
        );
        return registrar;
    }

}
