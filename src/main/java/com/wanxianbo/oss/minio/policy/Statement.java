package com.wanxianbo.oss.minio.policy;

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
public class Statement {

    private String sid;

    private String effect;

    private List<String> resource;

    @Singular("act")
    private List<String> action;

    public String getEffect() {
        return effect;
    }

    public List<String> getResource() {
        return resource;
    }

    public List<String> getAction() {
        return action;
    }

    public String getSid() {
        return sid;
    }
}
