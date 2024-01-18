package com.g7.framework.exporter.client.conf;

import java.util.List;

/**
 * .::::.
 * .::::::::.
 * :::::::::::
 * ..:::::::::::'
 * '::::::::::::'
 * .::::::::::
 * ':::::::::::::::..
 * ..:::::::::::::.
 * ``:::::::::::::::::
 * ::::``:::::::::'        .:::.
 * ::::'   ':::::'       .::::::::.
 * .::::'      ::::     .:::::::'::::.
 * .:::'       :::::  .:::::::::' ':::::.
 * .::'        :::::.:::::::::'      ':::::.
 * .::'         ::::::::::::::'         ``::::.
 * ...:::           ::::::::::::'              ``::.
 * ```` ':.          ':::::::::'                  ::::..
 * '.:::::'                    ':'````..
 * @author dreamyao
 * @date 2019/9/1 上午11:25
 */
public class ProviderClientConfig {
    /**
     * 全路径访问不用配置，非全路径配置包
     */
    private String packageHead;
    /**
     * 安全接口包配置  packageHead=“”有效
     */
    private List<String> safePackage;

    /**
     * 统计consumer时 来源方式：2个值 "appid" or "ip" 默认 "appid"
     */
    private String consumerSourceType = "appid";

    public String getConsumerSourceType() {
        return consumerSourceType;
    }

    public void setConsumerSourceType(String consumerSourceType) {
        this.consumerSourceType = consumerSourceType;
    }

    public List<String> getSafePackage() {
        return safePackage;
    }

    public void setSafePackage(List<String> safePackage) {
        this.safePackage = safePackage;
    }

    public String getPackageHead() {
        return packageHead;
    }

    public void setPackageHead(String packageHead) {
        this.packageHead = packageHead;
    }
}
