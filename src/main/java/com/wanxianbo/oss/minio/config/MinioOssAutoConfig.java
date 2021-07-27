package com.wanxianbo.oss.minio.config;

import com.wanxianbo.oss.minio.MinioOssApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 *
 * </p>
 *
 * @author wanxinabo
 * @date 2021/7/27
 */
@Configuration
@EnableConfigurationProperties(value = MinioProperties.class)
@ConditionalOnClass(MinioOssApi.class)
public class MinioOssAutoConfig {

    @Autowired
    private MinioProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public MinioOssApi getMinioOssApi() {
        return new MinioOssApi(properties);
    }
}
