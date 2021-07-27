package com.wanxianbo.oss.minio.config;

import io.minio.credentials.Credentials;
import io.minio.messages.ResponseDate;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author wanxinabo
 * @date 2021/7/26
 */
@Root(name = "Credentials", strict = false)
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
