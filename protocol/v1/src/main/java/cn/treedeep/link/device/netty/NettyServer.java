package cn.treedeep.link.device.netty;

import cn.treedeep.link.config.LinkConfig;
import cn.treedeep.link.device.protocol.codec.FrameDecoder;
import cn.treedeep.link.device.protocol.codec.FrameEncoder;
import cn.treedeep.link.protocol.v1.Protocol;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>NettyServer</p>
 *
 * @author 周广明
 * @since 2025/3/29 22:22
 */
@Slf4j
@Component("p_v1_NettyServer")
public class NettyServer {

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private final LinkConfig linkConfig;
    private final ServerHandler serverHandler;

    @Autowired
    public NettyServer(LinkConfig linkConfig, ServerHandler serverHandler) {
        this.linkConfig = linkConfig;
        this.serverHandler = serverHandler;
    }

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            start();
        } catch (Exception e) {
            log.error("Netty服务器启动失败，将关闭整个应用", e);
            // 获取Spring的退出码bean
            org.springframework.boot.ExitCodeGenerator exitCodeGenerator = () -> 1;
            // 关闭Spring应用
            org.springframework.boot.SpringApplication.exit(applicationContext, exitCodeGenerator);
            // 强制退出JVM
            System.exit(1);
        }
    }

    public void start() throws InterruptedException {
        Class<? extends ServerChannel> channelClass;

        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(1);  // Linux
            workerGroup = new EpollEventLoopGroup();
            channelClass = EpollServerSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            bossGroup = new KQueueEventLoopGroup(1); // macOS/BSD
            workerGroup = new KQueueEventLoopGroup();
            channelClass = KQueueServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup(1);    // 其他平台
            workerGroup = new NioEventLoopGroup();
            channelClass = NioServerSocketChannel.class;
        }

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Netty：正在关闭服务器...");
            stop();
            System.out.println("Netty：服务器已关闭");
        }));

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(channelClass)
                .option(ChannelOption.SO_BACKLOG, 128)
                // 增加服务端接收缓冲区大小
                .option(ChannelOption.SO_RCVBUF, 1024 * 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                // 增加客户端连接的接收缓冲区大小
                .childOption(ChannelOption.SO_RCVBUF, 1024 * 1024)
                // 增加写缓冲区大小
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024, 4 * 1024 * 1024))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(Protocol.lengthFieldBasedFrameDecoder())
                                .addLast(new FrameDecoder())
                                .addLast(new FrameEncoder())
                                .addLast(serverHandler);
                    }
                });

        try {
            // 使用await()而不是sync()，这样可以捕获绑定异常
            ChannelFuture f = b.bind(linkConfig.getServerPort());
            f.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("Netty服务器启动成功，监听端口：{}", linkConfig.getServerPort());
                } else {
                    log.error("Netty服务器启动失败，无法绑定端口：{}", linkConfig.getServerPort(), future.cause());
                    // 关闭Spring应用
                    org.springframework.boot.ExitCodeGenerator exitCodeGenerator = () -> 1;
                    org.springframework.boot.SpringApplication.exit(applicationContext, exitCodeGenerator);
                    System.exit(1);
                }
            });
            
            // 等待服务器关闭
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("Netty服务器启动过程中发生异常", e);
            throw e;
        }
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}