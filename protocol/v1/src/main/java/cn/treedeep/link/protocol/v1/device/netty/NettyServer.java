package cn.treedeep.link.protocol.v1.device.netty;

import cn.treedeep.link.protocol.v1.device.protocol.codec.FrameDecoder;
import cn.treedeep.link.protocol.v1.device.protocol.codec.FrameEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
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
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Netty：正在关闭服务器...");
            stop();
            System.out.println("Netty：服务器已关闭");
        }));

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
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