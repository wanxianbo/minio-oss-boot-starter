package com.wanxianbo.oss.minio.policy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Singular;

import java.util.List;

/**
 * <p>
 *  {
 *     "Version": "2012-10-17",
 *     "Statement": [
 *         {
 *             "Action": ["admin:*"],
 *             "Effect": "Allow",
 *             "Sid": ""
 *         },
 *         {
 *             "Action": ["s3:*"],
 *             "Effect": "Allow",
 *             "Resource": ["arn:aws:s3:::*"],
 *             "Sid": ""
 *         }
 *     ]
 * }
 * </p>
 *
 * @author 邱理 WHRDD-PC104
 * @since 2021/4/8
 */
@Builder
@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
public class PolicyFile {

    @Builder.Default
    private final String version = "2012-10-17";

    @Singular("state")
    private List<Statement> statement;

    public String getVersion() {
        return version;
    }

    public List<Statement> getStatement() {
        return statement;
    }

    public String policyString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
