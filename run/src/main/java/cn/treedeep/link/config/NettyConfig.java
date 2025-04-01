package cn.treedeep.link.config;

import cn.treedeep.link.protocol.v1.device.netty.NettyServer;
import cn.treedeep.link.protocol.v1.device.netty.ServerHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NettyConfig {

    @Autowired
    private LinkConfig linkConfig;

    @Bean
    public NettyServer nettyServer(ServerHandler serverHandler) {
        return new NettyServer(linkConfig.getServerPort(), serverHandler);
    }
}
