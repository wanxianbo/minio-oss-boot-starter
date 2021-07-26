package com.wanxianbo.oss.minio;

import com.wanxianbo.oss.minio.config.CustomCredentials;
import com.wanxianbo.oss.minio.config.MinioProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.StringJoiner;

/**
 * <p>
 *
 * </p>
 *
 * @author 邱理 WHRDD-PC104
 * @since 2021/4/9
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinioOssToken {

    public static MinioOssToken from(CustomCredentials credentials, MinioProperties properties) {
        return new MinioOssToken(credentials.accessKey(),
                credentials.secretKey(),
                credentials.sessionToken(),
                credentials.expirationTime().withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(),
                properties.getEndpoint(),
                properties.getRegion(),
                properties.getBucket(),
                properties.getPublicBucket());
    }

    private String accessKey;

    private String secretKey;

    private String sessionToken;

    private LocalDateTime expiration;

    private String endpoint;

    private String region;

    private String bucket;

    private String publicBucket;

    @Override
    public String toString() {
        return new StringJoiner(", ", MinioOssToken.class.getSimpleName() + "[", "]")
                .add("accessKey='" + accessKey + "'")
                .add("secretKey='" + secretKey + "'")
                .add("sessionToken='" + sessionToken + "'")
                .add("expiration=" + expiration)
                .add("endpoint='" + endpoint + "'")
                .add("bucket='" + bucket + "'")
                .add("publicBucket='" + publicBucket + "'")
                .toString();
    }
}
