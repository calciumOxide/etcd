package com.oxide.etcd.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ToString
@ConfigurationProperties(EtcdConfig.ETCD)
public class EtcdConfig {

    public static final String ETCD = "etcd";

    /**
     * ETCD server url
     */
    private String url;

    /**
     * 配置路径前缀
     */
    private String propertyPrefix;

    /**
     * ETCD登录用户名
     */
    private String username;

    /**
     * ETCD登录密码
     */
    private String password;

    /**
     * 是否开始ETCD
     */
    private boolean enabled = true;

    /**
     * 是否开启认证登录
     */
    private boolean isAuth = false;

}
