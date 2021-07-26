package com.wanxianbo.oss.minio.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * <p>
 *
 * </p>
 *
 * @author wanxinabo
 * @date 2021/7/26
 */
@ConfigurationProperties("minio")
@Data
public class MinioProperties {

    private String key;

    private String secret;

    private String bucket;

    private String publicBucket;

    private String endpoint;

    private String region = "cn-nanchang-1";

    private Duration tokenDuration;

    private List<String> policyActions;

    private List<String> publicPolicyActions;

}
