package cn.treedeep.link.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置类
 * 从配置文件中读取应用参数
 */
@Configuration("td_link_LinkConfig")
@ConfigurationProperties(prefix = "link")
@Data
public class LinkConfig {
    
    /**
     * 会话超时时间（分钟）
     */
    private int sessionTimeoutMinutes = 10;
    
    /**
     * 上传文件保存路径
     */
    private String uploadPath = "uploads";
    
    /**
     * 服务器端口
     */
    private int serverPort = 9900;
    
    /**
     * 心跳间隔（秒）
     */
    private int heartbeatInterval = 30;
}