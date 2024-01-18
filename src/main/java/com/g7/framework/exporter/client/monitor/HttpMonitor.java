package com.g7.framework.exporter.client.monitor;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.MonitorConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.monitor.Monitor;
import com.alibaba.dubbo.monitor.MonitorFactory;
import com.alibaba.dubbo.monitor.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Objects;

/**
 * @author dreamyao
 * @title
 * @date 2019/9/1 上午11:25
 * @since 1.0.0
 */
public class HttpMonitor {

    /**
     * 是否已初始化
     */
    private static boolean inited = false;
    private static HttpMonitor instance = null;
    private static final Logger logger = LoggerFactory.getLogger(HttpMonitor.class);
    private Monitor monitor;
    private volatile String application;
    /**
     * 本地 容器（jetty）端口
     */
    private int localPort;
    private ApplicationContext applicationContext;
    /**
     * 构造函数
     * @param applicationContext
     * @param localPort
     */
    private HttpMonitor(ApplicationContext applicationContext, int localPort) {
        this.applicationContext = applicationContext;
        this.localPort = localPort;
        httpMonitorInit();
    }

    /**
     * 初始化
     * @param applicationContext
     * @param localPort
     * @return
     */
    public synchronized static HttpMonitor init(ApplicationContext applicationContext, int localPort) {
        if (Boolean.FALSE.equals(inited)) {
            inited = true;
            instance = new HttpMonitor(applicationContext, localPort);
        }
        return instance;
    }

    /**
     * 判断是否初始化
     * @return
     */
    public static boolean isInited() {
        return inited;
    }

    /**
     * 获取实例
     * @return
     */
    public static HttpMonitor getInstance() {
        return instance;
    }

    private void httpMonitorInit() {

        MonitorConfig mc;
        try {
            mc = applicationContext.getBean(MonitorConfig.class);
        } catch (Exception e) {
            logger.warn("无Monitor配置");
            return;
        }
        try {
            RegistryConfig rc = applicationContext.getBean(RegistryConfig.class);
            ApplicationConfig ac = applicationContext.getBean(ApplicationConfig.class);
            MyProviderConfig myProviderConfig = new MyProviderConfig();
            myProviderConfig.setRegistry(rc);
            myProviderConfig.setMonitor(mc);
            myProviderConfig.setApplication(ac);

            URL url = myProviderConfig.getMoniURL();
            application = ac.getName();

            if (Objects.nonNull(url)) {

                String path = url.getAbsolutePath();
                logger.info(path);

                ExtensionLoader<MonitorFactory> loader = ExtensionLoader.getExtensionLoader(MonitorFactory.class);
                MonitorFactory mf = loader.getDefaultExtension();

                monitor = mf.getMonitor(url);
                logger.debug("httpprovider-Monitor配置初始化成功");
            }

        } catch (Exception e) {
            logger.error("初始化失败:", e);
            monitor = null;
        }
    }

    public void collect(String remoteValue, String service, String method, long start, boolean error) {
        if (this.monitor == null) {
            return;
        }
        try {

            // 计算调用耗时
            long elapsed = System.currentTimeMillis() - start;

            //不统计并发
            int concurrent = 1;

            String remoteKey = MonitorService.CONSUMER;
            String input = "", output = "";
            monitor.collect(new URL(Constants.COUNT_PROTOCOL,
                    NetUtils.getLocalHost(), localPort,
                    service + "/" + method,
                    MonitorService.APPLICATION, application,
                    MonitorService.INTERFACE, service,
                    MonitorService.METHOD, method,
                    remoteKey, remoteValue,
                    error ? MonitorService.FAILURE : MonitorService.SUCCESS, "1",
                    MonitorService.ELAPSED, String.valueOf(elapsed),
                    MonitorService.CONCURRENT, String.valueOf(concurrent),
                    Constants.INPUT_KEY, input,
                    Constants.OUTPUT_KEY, output));
        } catch (Throwable t) {
            logger.error("Failed to monitor count service " + monitor.getUrl() + ", cause: " + t.getMessage(), t);
        }
    }

    private class MyProviderConfig extends ProviderConfig {

        private URL getMoniURL() {
            List<URL> us = loadRegistries(false);
            if (us != null && us.size() > 0) {
                for (URL u : us) {
                    URL monitorUrl = loadMonitor(u);
                    if (monitorUrl != null) {
                        return monitorUrl;
                    }
                }
            }
            return null;
        }
    }
}
