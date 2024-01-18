package com.g7.framework.exporter.client.signal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import sun.misc.Signal;

/**
 * @author dreamyao
 * @title 信号量监听器，处理外部信号的传入处理
 * @date 2019/9/1 上午11:25
 * @since 1.0.0
 */
@Service
public class SignalRegisterListener implements ApplicationListener<ContextRefreshedEvent> {

    private Logger logger = LoggerFactory.getLogger(SignalRegisterListener.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        String os = System.getProperty("os.name");
        if (!os.toLowerCase().startsWith("win")) {
            Signal.handle(new Signal("USR2"), new TermSignalHandler());
            logger.info("成功注册了信号量处理器:USR2");
        }
    }
}
