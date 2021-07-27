package com.wanxianbo.oss.minio;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.wanxianbo.oss.minio.config.CustomAssumeRoleProvider;
import com.wanxianbo.oss.minio.config.CustomCredentials;
import com.wanxianbo.oss.minio.config.MinioProperties;
import com.wanxianbo.oss.minio.policy.PolicyFile;
import com.wanxianbo.oss.minio.policy.Statement;
import io.minio.*;
import io.minio.credentials.StaticProvider;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * <p>
 *
 * </p>
 *
 * @author wanxinabo
 * @date 2021/7/26
 */
@Slf4j
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
        statementBuilder.resource(Lists.newArrayList(String.format("arn:aws:s3:::%s/*", properties.getBucket())));
        Statement.StatementBuilder publicStatementBuilder = Statement.builder()
                .sid("Default_Client_Policy_Public").effect("Allow");
        properties.getPublicPolicyActions().forEach(publicStatementBuilder::act);
        statementBuilder.resource(Lists.newArrayList(String.format("arn:aws:s3:::%s/*", properties.getPublicBucket())));
        builder.state(statementBuilder.build())
                .state(publicStatementBuilder.build()).build().policyString();
    }

    public MinioOssToken temporaryOssToken() {
        try {
            CustomAssumeRoleProvider roleProvider = new CustomAssumeRoleProvider(
                    properties.getEndpoint(),
                    properties.getKey(),
                    properties.getSecret(),
                    (int) properties.getTokenDuration().getSeconds(),
                    policy, properties.getRegion(), null, "DefaultSession", null, null
            );
            return MinioOssToken.from((CustomCredentials) roleProvider.fetch(), properties);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("not expected error:", e);
        }
    }


    public String generateObjectKey(String id, boolean isPublic, @Nullable String prefix, @Nullable String suffix) {
        String info = String.format("new-oss:%s:%s", isPublic ? "public" : "private", id);
        String base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(info.getBytes());
        StringBuilder builder = new StringBuilder();
        if (!Strings.isNullOrEmpty(prefix)) builder.append(prefix).append("__");
        builder.append(base64);
        if (!Strings.isNullOrEmpty(suffix)) builder.append(".").append(suffix);
        return builder.toString();
    }

    public Map<String, String> presignedFormData(String objectName, PostPolicy updatePolicy)
            throws ErrorResponseException {
        Preconditions.checkState(!Strings.isNullOrEmpty(objectName));
        Preconditions.checkNotNull(updatePolicy);
        updatePolicy.addEqualsCondition("key", objectName);
        try {
            Map<String, String> result = client.getPresignedPostFormData(updatePolicy);
            result.put("bucketUrl", UriComponentsBuilder
                    .fromHttpUrl(properties.getEndpoint())
                    .path(updatePolicy.bucket()).toUriString());
            result.put("bucket", updatePolicy.bucket());
            result.put("key", objectName);
            return result;
        } catch (ErrorResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String presignedObjectUrl(String objectName, @Nullable String bucketName, @Nullable Duration duration)
            throws ErrorResponseException {
        try {
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET).object(objectName)
                    .region(properties.getRegion())
                    .bucket(ofNullable(bucketName).orElseGet(properties::getBucket));
            ofNullable(duration).map(Duration::getSeconds).map(Long::intValue).ifPresent(builder::expiry);
            return client.getPresignedObjectUrl(builder.build());
        } catch (ErrorResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MinioClient getClient() {
        return client;
    }

    public ObjectWriteResponse uploadData(InputStream stream, String objectName) throws ErrorResponseException {
        return uploadData(stream, objectName, null, null);
    }

    public ObjectWriteResponse uploadData(InputStream stream, String objectName,
                                          @Nullable String contentType,
                                          @Nullable String bucketName) throws ErrorResponseException {
        try {
            int size = stream.available();
            int partSize = -1;
            if (size == 0) {
                size = -1;
                partSize = DEFAULT_PART_SIZE;
            }
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .preloadData(true)
                    .object(objectName)
                    .bucket(nullableBucket(bucketName))
                    .stream(stream, size, partSize);
            ofNullable(contentType).ifPresent(builder::contentType);
            return client.putObject(builder.build());
        } catch (ErrorResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadData(OutputStream stream, String objectName, @Nullable String bucketName)
            throws ErrorResponseException {
        try {
            GetObjectResponse response = client.getObject(GetObjectArgs.builder()
                    .object(objectName)
                    .bucket(nullableBucket(bucketName)).build());
            copyTo(response, stream);
        } catch (ErrorResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyTo(InputStream input, OutputStream output) throws IOException {
        if (!(output instanceof BufferedOutputStream))
            output = new BufferedOutputStream(output);
        new ByteSource() {
            @Override
            public InputStream openStream() {
                return input;
            }
        }.copyTo(output);
        output.flush();
    }

    public static Logger getLog() {
        return log;
    }

    public ObjectWriteResponse uploadLargeData(InputStream stream, String objectName,
                                               @Nullable String contentType,
                                               @Nullable Integer partSize,
                                               @Nullable String bucketName) throws ErrorResponseException {

        PutObjectArgs.Builder builder = PutObjectArgs.builder()
                .preloadData(true)
                .stream(stream, -1, ofNullable(partSize).orElse(DEFAULT_PART_SIZE))
                .bucket(nullableBucket(bucketName))
                .object(objectName);
        ofNullable(contentType).ifPresent(builder::contentType);

        try {
            return client.putObject(builder.build());
        } catch (ErrorResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String nullableBucket(String nullable) {
        return ofNullable(nullable).orElseGet(properties::getBucket);
    }



    public static void main(String[] args) throws Exception {
        MinioProperties properties = new MinioProperties();
        properties.setKey("6WFzEtjjqdrE4puU9mGz");
        properties.setSecret("D6kDtnFL6THVvLXr98ioLgLJYLq9auFzMNt");
        properties.setBucket("private-bucket");
        properties.setPublicBucket("public-bucket");
        properties.setEndpoint("http://192.168.59.128:9011");
        properties.setRegion("cn-nanchang-1");
        properties.setTokenDuration(Duration.ofMinutes(15));
        properties.setPolicyActions(Lists.newArrayList("s3:Get*"));
        properties.setPublicPolicyActions(Lists.newArrayList("s3:Get*", "s3:PutObject", "s3:PutObjectRetention"));
        MinioOssApi minioOssApi = new MinioOssApi(properties);

        // 生成url测试
        String url = minioOssApi.presignedObjectUrl("test-demo", "private-bucket", Duration.ofMinutes(5));
        System.out.println(url);
        // upload文件测试
        minioOssApi.uploadData(new BufferedInputStream(Files.newInputStream(Paths.get("C:\\Users\\Dell\\Pictures\\Camera Roll\\Snipaste_2021-05-12_15-32-34.png"))), "test-demo-02");
        // down文件测试
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        minioOssApi.downloadData(outputStream, "test-demo-02","private-bucket");
        System.out.println(outputStream.toByteArray().length);
    }
}
