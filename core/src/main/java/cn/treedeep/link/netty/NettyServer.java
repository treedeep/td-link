package cn.treedeep.link.netty;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

public interface NettyServer extends ApplicationListener<ApplicationReadyEvent> {

    void start() throws InterruptedException;

    void stop();

    boolean isPrimary();
}
