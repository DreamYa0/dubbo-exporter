package com.g7.framework.exporter.client.context;

/**
 * @author dreamyao
 * @title http访问上下文
 * @date 2019/9/1 上午11:25
 * @since 1.0.0
 */
public class HttpProviderContext {

    private static final ThreadLocal<HttpProviderContext> context = new ThreadLocal<HttpProviderContext>();
    /**
     * 调用者标示
     */
    private String appid;
    /**
     * 调用者ip
     */
    private String ip;


    private HttpProviderContext(String appid, String ip) {
        this.appid = appid;
        this.ip = ip;
    }

    public static HttpProviderContext getContext() {
        return context.get();
    }

    public static void setContext(String appid, String ip) {
        context.set(new HttpProviderContext(appid, ip));
    }

    public String getAppid() {
        return appid;
    }

    public void setAppid(String appid) {
        this.appid = appid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
