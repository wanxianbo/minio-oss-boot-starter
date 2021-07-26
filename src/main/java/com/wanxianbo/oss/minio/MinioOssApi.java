package com.wanxianbo.oss.minio;

import com.wanxianbo.oss.minio.config.MinioProperties;
import com.wanxianbo.oss.minio.policy.PolicyFile;
import com.wanxianbo.oss.minio.policy.Statement;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.credentials.StaticProvider;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import javax.annotation.Nullable;

/**
 * <p>
 *
 * </p>
 *
 * @author wanxinabo
 * @date 2021/7/26
 */
@EnableConfigurationProperties(value = MinioProperties.class)
//@Slf4j
public class MinioOssApi {

    private static final Integer DEFAULT_PART_SIZE = 10 * 1024 * 1024;

    private final MinioProperties properties;

    private String policy;

    private MinioClient client;

    public MinioOssApi(MinioProperties properties) {
        this.properties = properties;
        buildAssumeRolePolicy();
        buildMinioClient();
    }

    private void buildMinioClient() {
        client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .region(properties.getRegion())
                .credentialsProvider(new StaticProvider(properties.getKey(), properties.getSecret(), null))
                .build();
    }

    private void buildAssumeRolePolicy() {
        if (CollectionUtils.isEmpty(properties.getPolicyActions())) return;
        PolicyFile.PolicyFileBuilder builder = PolicyFile.builder();
        Statement.StatementBuilder statementBuilder = Statement.builder()
                .sid("Default_Client_Policy").effect("Allow");
        properties.getPolicyActions().forEach(statementBuilder::act);
        statementBuilder.resource(String.format("arn:aws:s3:::%s/*", properties.getBucket()));
        Statement.StatementBuilder publicStatementBuilder = Statement.builder()
                .sid("Default_Client_Policy_Public").effect("Allow");
        properties.getPublicPolicyActions().forEach(publicStatementBuilder::act);
        statementBuilder.resource(String.format("arn:aws:s3:::%s/*", properties.getPublicBucket()));
        builder.state(statementBuilder.build())
                .state(publicStatementBuilder.build()).build().policyString();

    }


    public String generateObjectKey(String id, boolean isPublic, @Nullable String prefix, @Nullable String suffix) {
        return null;
    }


    public static void main(String[] args) throws Exception {
        MinioClient minioClient = MinioClient.builder().endpoint("http://192.168.59.128:9011/")
                .credentials("minioadmin", "minioadmin")
                .region("cn-nanchang-1")
                .build();
// 6WFzEtjjqdrE4puU9mGz D6kDtnFL6THVvLXr98ioLgLJYLq9auFzMNt
//        minioClient.uploadObject(UploadObjectArgs.builder()
//                .bucket("private-bucket")
//                .object("test-demo")
//                .filename("C:\\Users\\Dell\\Pictures\\Camera Roll\\45d56467c81347819a1e1a0a252373e1!400x400.jpeg")
//                .build());

        // 在获取一下
//        String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
//                .method(Method.GET)
//                .bucket("public-bucket")
//                .object("test-demo")
//                .build());

                String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket("private-bucket")
                .object("test-demo")
                .build());

        System.out.println(url);
    }
}
