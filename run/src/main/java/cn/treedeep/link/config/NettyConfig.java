package cn.treedeep.link.config;

import cn.treedeep.link.device.netty.Pv1NettyServer;
import cn.treedeep.link.device.netty.ServerHandler;
import cn.treedeep.link.netty.NettyServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyConfig {

    @Bean
    public NettyServer createNettyServer(LinkConfig linkConfig, ServerHandler serverHandler) {
        return new Pv1NettyServer(true, linkConfig, serverHandler);
    }
}
