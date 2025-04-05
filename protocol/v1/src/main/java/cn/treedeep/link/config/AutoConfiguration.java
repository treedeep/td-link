package cn.treedeep.link.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration("td_link_V1AutoConfiguration")
@EnableAsync
@ComponentScan(basePackages = {"cn.treedeep.link"})
@ConfigurationPropertiesScan({"cn.treedeep.link"})
public class AutoConfiguration {
}