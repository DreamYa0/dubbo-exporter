package com.g7.framework.exporter.client.signal;

import com.alibaba.dubbo.config.ProtocolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * @author dreamyao
 * @title TERM 系统信号量处理
 * @date 2019/9/1 上午11:25
 * @since 1.0.0
 */
public class TermSignalHandler implements SignalHandler {

    private Logger logger = LoggerFactory.getLogger(TermSignalHandler.class);

    @Override
    public void handle(Signal signal) {

        logger.info("接收到信号量:" + signal.getName() + "，开始处理...");
        //注销所有的
        ProtocolConfig.destroyAll();
        logger.info("信号量:" + signal.getName() + "处理完毕...");
    }
}
