package cn.treedeep.link.config;

import cn.treedeep.link.device.client.SimulatorManager;
import cn.treedeep.link.device.netty.Pv1NettyServer;
import cn.treedeep.link.device.netty.Pv1ServerHandler;
import cn.treedeep.link.device.protocol.model.command.CmdHeartbeat;
import cn.treedeep.link.event.DeviceEventPublisher;
import cn.treedeep.link.netty.ChannelManager;
import cn.treedeep.link.netty.FileUploadManager;
import cn.treedeep.link.netty.NettyServer;
import cn.treedeep.link.netty.SessionManager;
import cn.treedeep.link.service.DeviceService;
import cn.treedeep.link.service.DeviceServiceImpl;
import cn.treedeep.link.task.SessionCleanupTask;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /**
     * 创建Netty服务
     */
    @Bean
    public NettyServer createNettyServer(LinkConfig linkConfig,
                                         Pv1ServerHandler serverHandler) {
        return new Pv1NettyServer(true, linkConfig, serverHandler);
    }

    /**
     * 创建设备服务
     */
    @Bean
    public DeviceService createDeviceService(SessionManager sessionManager,
                                             ChannelManager channelManager) {
        return new DeviceServiceImpl(channelManager, sessionManager);
    }

    /**
     * 开启会话清理
     */
    @Bean
    public SessionCleanupTask createSessionCleanupTask(LinkConfig linkConfig,
                                                       SessionManager sessionManager,
                                                       ChannelManager channelManager,
                                                       DeviceEventPublisher eventPublisher) {
        return new SessionCleanupTask(linkConfig, sessionManager, channelManager, eventPublisher);
    }



    @Bean
    public Pv1ServerHandler createPv1ServerHandler(SessionManager sessionManager,
                                                   ChannelManager channelManager,
                                                   FileUploadManager fileUploadManager,
                                                   DeviceEventPublisher eventPublisher) {
        return new Pv1ServerHandler(sessionManager, channelManager, fileUploadManager, eventPublisher);
    }

    @Bean
    public SessionManager createSessionManager() {
        return new SessionManager();
    }

    @Bean
    public ChannelManager createChannelManager() {
        return new ChannelManager();
    }

    @Bean
    public FileUploadManager createFileUploadManager(LinkConfig config) {
        return new FileUploadManager(config);
    }

    @Bean
    public DeviceEventPublisher createDeviceEventPublisher() {
        return new DeviceEventPublisher();
    }

    @Bean
    public SimulatorManager createSimulatorManager(LinkConfig config) {
        return new SimulatorManager(config);
    }

    @Bean
    @ConditionalOnProperty(name = "link.heartbeatDetection", havingValue = "true")
    public CmdHeartbeat createServerHeartbeat() {
        return new CmdHeartbeat();
    }

}
