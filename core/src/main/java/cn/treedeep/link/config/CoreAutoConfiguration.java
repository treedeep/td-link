package cn.treedeep.link.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@ComponentScan(basePackages = {"cn.treedeep.link.core"})
@ConfigurationPropertiesScan({"cn.treedeep.link.core"})
public class CoreAutoConfiguration {
}