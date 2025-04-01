package cn.treedeep.link.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@ComponentScan(basePackages = {"cn.treedeep.link.protocol.v1"})
@ConfigurationPropertiesScan({"cn.treedeep.link.protocol.v1"})
public class AutoConfiguration {
}