package com.g7.framework.exporter.client.conf;

import com.g7.framework.exporter.client.exporter.ApiExporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExporterAutoConfiguration {

    @Bean
    public ProviderClientConfig providerClientConfig() {
        ProviderClientConfig config = new ProviderClientConfig();
        config.setPackageHead("");
        return config;
    }

    @Bean
    public ApiExporter apiController() {
        // 让dubbo服务可以通过http访问，方便开发本地测试
        return new ApiExporter();
    }
}
