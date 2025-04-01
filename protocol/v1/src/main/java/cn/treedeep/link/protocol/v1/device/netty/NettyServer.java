package cn.treedeep.link.protocol.v1.device.netty;

import cn.treedeep.link.protocol.v1.device.protocol.codec.FrameDecoder;
import cn.treedeep.link.protocol.v1.device.protocol.codec.FrameEncoder;
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
import lombok.extern.slf4j.Slf4j;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>NettyServer</p>
 *
 * @author 周广明
 * @since 2025/3/29 22:22
 */
@Slf4j
public class NettyServer {

    private final int port;
    private final ServerHandler serverHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public NettyServer(int port, ServerHandler serverHandler) {
        this.port = port;
        this.serverHandler = serverHandler;
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
                                .addLast(new FrameDecoder())
                                .addLast(new FrameEncoder())
                                .addLast(serverHandler);
                    }
                });

        ChannelFuture f = b.bind(port).sync();
        log.info("Netty服务器启动成功，监听端口：{}", port);

        f.channel().closeFuture().sync();
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}