package com.wanxianbo.oss.minio.config;

import io.minio.credentials.Credentials;
import io.minio.messages.ResponseDate;
import org.jetbrains.annotations.NotNull;
import org.simpleframework.xml.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
public class CustomCredentials extends Credentials {

    private final ResponseDate expirationDate;

    public CustomCredentials(
            @Nonnull @Element(name = "AccessKeyId") String accessKey,
            @Nonnull @Element(name = "SecretAccessKey") String secretKey,
            @Nullable @Element(name = "SessionToken") String sessionToken,
            @Nullable @Element(name = "Expiration") ResponseDate expiration) {
        super(accessKey, secretKey, sessionToken, expiration);
        this.expirationDate = expiration;
    }

    public ZonedDateTime expirationTime() {
        if (expirationDate != null)
            return expirationDate.zonedDateTime();
        return null;
    }
}
