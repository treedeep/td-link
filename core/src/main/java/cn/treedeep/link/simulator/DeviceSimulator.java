package cn.treedeep.link.simulator;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Data;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>终端设备模拟器</p>
 *
 * @author 周广明
 * @since 2025/4/3 11:50
 */
@Data
public abstract class DeviceSimulator {
    protected final int deviceId;
    protected short sessionId;
    protected int taskId;
    protected Channel channel;
    protected final EventLoopGroup group;
    protected final Class<? extends ServerChannel> channelClass;
    protected ScheduledFuture<?> heartbeatFuture;
    protected SimulatorStatus status = SimulatorStatus.CREATED;

    public DeviceSimulator(int deviceId) {
        this.deviceId = deviceId;

        if (Epoll.isAvailable()) {
            this.group = new EpollEventLoopGroup(1);  // Linux
            this.channelClass = EpollServerSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            this.group = new KQueueEventLoopGroup(1); // macOS/BSD
            channelClass = KQueueServerSocketChannel.class;
        } else {
            this.group = new NioEventLoopGroup(1);    // 其他平台
            channelClass = NioServerSocketChannel.class;
        }
    }

    public void startHeartbeat() {
        if (heartbeatFuture != null) {
            return;
        }
        heartbeatFuture = group.scheduleAtFixedRate(
                this::sendHeartbeat,
                0,
                30,
                TimeUnit.SECONDS
        );
    }

    public void stopHeartbeat() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
            heartbeatFuture = null;
        }
    }

    public void writeData(byte[] data) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(data);
        }
    }


    public abstract void connect(String serverHost, int serverPort);

    protected abstract void sendRegisterRequest();

    public abstract void sendHeartbeat();

    public abstract void uploadFile(String filePath);

    public abstract void disconnect();

}
