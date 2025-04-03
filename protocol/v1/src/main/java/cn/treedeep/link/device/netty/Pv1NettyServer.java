package cn.treedeep.link.device.netty;

import cn.treedeep.link.config.LinkConfig;
import cn.treedeep.link.device.protocol.codec.FrameDecoder;
import cn.treedeep.link.device.protocol.codec.FrameEncoder;
import cn.treedeep.link.netty.NettyServer;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>NettyServer，SpringBoot启动后 自启动</p>
 *
 * @author 周广明
 * @since 2025/3/29 22:22
 */
@Slf4j
public class Pv1NettyServer implements NettyServer {

    @Getter
    private final boolean primary;
    private final LinkConfig linkConfig;
    private final Pv1ServerHandler serverHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * 构造函数，用于初始化NettyServer。
     *
     * @param primary       是否主要服务？是，只有Netty服务正常启动整项目才可以启动。
     * @param linkConfig    LinkConfig对象，用于获取服务器端口号。
     * @param serverHandler ServerHandler对象，用于处理服务器事件。
     */
    public Pv1NettyServer(boolean primary, LinkConfig linkConfig, Pv1ServerHandler serverHandler) {
        this.primary = primary;
        this.linkConfig = linkConfig;
        this.serverHandler = serverHandler;
    }

    @Override
    public void start() throws InterruptedException {
        start(null);
    }

    @Override
    public void stop() {
        System.out.println("Netty：正在关闭服务器...");
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        System.out.println("Netty：服务器已关闭");
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        if (primary) {
            try {
                start(event.getApplicationContext());
            } catch (Exception e) {
                log.error("Netty服务器启动失败，将关闭整个应用", e);
                // 获取Spring的退出码bean
                ExitCodeGenerator exitCodeGenerator = () -> 1;
                // 关闭Spring应用
                SpringApplication.exit(event.getApplicationContext(), exitCodeGenerator);
                // 强制退出JVM
                System.exit(1);
            }
        } else {
            try {
                start();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void start(ApplicationContext applicationContext) throws InterruptedException {
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
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

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
                    if (applicationContext != null) {
                        ExitCodeGenerator exitCodeGenerator = () -> 1;
                        SpringApplication.exit(applicationContext, exitCodeGenerator);
                        System.exit(1);
                    }
                }
            });

            // 等待服务器关闭
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("Netty服务器启动过程中发生异常", e);
            throw e;
        }
    }

}