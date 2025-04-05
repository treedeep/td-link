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
     * 上传文件保存路径
     */
    private String uploadPath = ".link/uploads";

    /**
     * 服务器地址
     */
    private String serverHost = "127.0.0.1";

    /**
     * 服务器端口
     */
    private int serverPort = 9900;

    /**
     * 服务端心跳检测
     */
    private boolean serverHeartbeat = false;

    /**
     * 心跳检测间隔（秒）
     */
    private int heartbeatInterval = 30;

    /**
     * 清理过期会话
     */
    private boolean cleanupSessions = false;

    /**
     * 清理过期会话间隔（分钟）
     */
    private int cleanupExpiredSessions = 5;

    /**
     * 会话超时时间（分钟）
     */
    private int sessionTimeoutMinutes = 10;


}