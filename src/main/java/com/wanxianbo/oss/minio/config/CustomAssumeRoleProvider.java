package com.wanxianbo.oss.minio.config;

import io.minio.Digest;
import io.minio.Signer;
import io.minio.Time;
import io.minio.credentials.AssumeRoleBaseProvider;
import okhttp3.*;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * <p>
 *
 * </p>
 *
 * @author wanxinabo
 * @date 2021/7/26
 */
public class CustomAssumeRoleProvider extends AssumeRoleBaseProvider {

    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final String contentSha256;
    private final Request request;

    public CustomAssumeRoleProvider(@Nonnull String stsEndpoint,
                                    @Nonnull String accessKey,
                                    @Nonnull String secretKey,
                                    @Nullable Integer durationSeconds,
                                    @Nullable String policy,
                                    @Nullable String region,
                                    @Nullable String roleArn,
                                    @Nullable String roleSessionName,
                                    @Nullable String externalId,
                                    @Nullable OkHttpClient customHttpClient) throws NoSuchAlgorithmException {
        super(customHttpClient);
        stsEndpoint = Objects.requireNonNull(stsEndpoint, "STS endpoint cannot be empty");
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(stsEndpoint), "Invalid STS endpoint");
        accessKey = Objects.requireNonNull(accessKey, "Access key must not be null");
        if (accessKey.isEmpty()) {
            throw new IllegalArgumentException("Access key must not be empty");
        }
        this.accessKey = accessKey;
        this.secretKey = Objects.requireNonNull(secretKey, "Secret key must not be null");
        this.region = (region != null) ? region : "";

        if (externalId != null && (externalId.length() < 2 || externalId.length() > 1224)) {
            throw new IllegalArgumentException("Length of ExternalId must be in between 2 and 1224");
        }

        String host = url.host() + ":" + url.port();
        // ignore port when port and service matches i.e HTTP -> 80, HTTPS -> 443
        if ((url.scheme().equals("http") && url.port() == 80)
                || (url.scheme().equals("https") && url.port() == 443)) {
            host = url.host();
        }

        HttpUrl.Builder urlBuilder =
                newUrlBuilder(
                        url,
                        "AssumeRole",
                        getValidDurationSeconds(durationSeconds),
                        policy,
                        roleArn,
                        roleSessionName);
        if (externalId != null) {
            urlBuilder.addQueryParameter("ExternalId", externalId);
        }

        String data = urlBuilder.build().encodedQuery();
        this.contentSha256 = Digest.sha256Hash(data);
        this.request =
                new Request.Builder()
                        .url(url)
                        .header("Host", host)
                        .method(
                                "POST",
                                RequestBody.create(data, MediaType.parse("application/x-www-form-urlencoded")))
                        .build();
    }

    @Override
    protected Request getRequest() {
        try {
            return Signer.signV4Sts(
                    this.request
                            .newBuilder()
                            .header("x-amz-date", ZonedDateTime.now().format(Time.AMZ_DATE_FORMAT))
                            .build(),
                    region,
                    accessKey,
                    secretKey,
                    contentSha256);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new ProviderException("Signature calculation failed", e);
        }
    }

    @Override
    protected Class<? extends AssumeRoleBaseProvider.Response> getResponseClass() {
        return CustomAssumeRoleResponse.class;
    }

    /**
     * Object representation of response XML of AssumeRole API.
     */
    @Root(name = "AssumeRoleResponse", strict = false)
    @Namespace(reference = "https://sts.amazonaws.com/doc/2011-06-15/")
    public static class CustomAssumeRoleResponse implements AssumeRoleBaseProvider.Response {
        @Path(value = "AssumeRoleResult")
        @Element(name = "Credentials")
        private CustomCredentials credentials;

        public CustomCredentials getCredentials() {
            return credentials;
        }
    }
}
