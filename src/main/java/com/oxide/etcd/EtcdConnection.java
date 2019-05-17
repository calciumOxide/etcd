package com.oxide.etcd;


import com.coreos.jetcd.Client;
import com.coreos.jetcd.Watch;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.options.WatchOption;
import com.coreos.jetcd.watch.WatchEvent;
import com.coreos.jetcd.watch.WatchResponse;
import com.oxide.etcd.annotation.Etcd;
import com.oxide.etcd.config.EtcdConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class EtcdConnection {

    private static String propertyPrefix = "";

    private static Client client;

    private static Yaml yaml = new Yaml();

    private static Map<String, Object> propertyKey = new HashMap<>();

    private static Map<Class, Object> propertyCatch = new HashMap<>();

    private static Map<Object, Long> propertyVersion = new HashMap<>();

    public static <T> T getEtcdProperty(Class<T> clazz) {
        Object property = propertyCatch.get(clazz);
        if (property == null) {
            try {
                property = clazz.newInstance();
                propertyCatch.put(clazz, property);
                propertyKey.put(clazz.getAnnotation(Etcd.class).WatchKey(), property);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return (T) property;
    }

    public static void connect(EtcdConfig etcdConfig) {

        if (etcdConfig.isEnabled()) {
            if (etcdConfig.isAuth()) {
                client = Client.builder().user(ByteSequence.fromString(etcdConfig.getUsername()))
                        .password(ByteSequence.fromString(etcdConfig.getPassword()))
                        .endpoints(etcdConfig.getUrl().split(",")).build();
            } else {
                client = Client.builder().endpoints(etcdConfig.getUrl().split(",")).build();
            }
            if (!StringUtils.isEmpty(etcdConfig.getPropertyPrefix())) {
                propertyPrefix = etcdConfig.getPropertyPrefix();
            }
            initProperty();
        }
    }

    public static void watch() {
        if (client == null) {
            log.info("etcd client is not init.");
            return;
        }
        if (propertyCatch.size() == 0) {
            log.info("etcd watch path is empty.");
            return;
        }
        Watch.Watcher watch = client.getWatchClient().watch(ByteSequence.fromString(""), WatchOption.newBuilder().withPrefix(ByteSequence.fromString(propertyPrefix)).build());
        (new Thread(() -> {
            while (true) {
                try {
                    WatchResponse response = watch.listen();
                    if (response == null || response.getEvents() == null) {
                        continue;
                    }
                    response.getEvents().forEach(e -> {
                        WatchEvent.EventType eventType = e.getEventType();
                        KeyValue keyValue = e.getKeyValue();
                        String key = keyValue.getKey().toStringUtf8();
                        String value = keyValue.getValue().toStringUtf8();
                        log.info("update property eventType={},key={},value={}.", eventType, key, value);

                        if (WatchEvent.EventType.PUT.equals(eventType)) {
                            inflateProperty(keyValue, propertyKey.get(key.replace(propertyPrefix, "")));
                        }
                    });
                } catch (Exception e) {
                    log.info("etcd refresh err.");
                }
            }

        })).start();
    }

    private static void initProperty() {
        try {
            propertyCatch.forEach((k, v) -> {
                try {
                    Etcd etcd = v.getClass().getAnnotation(Etcd.class);
                    CompletableFuture<GetResponse> future = client.getKVClient().get(ByteSequence.fromString(propertyPrefix + etcd.WatchKey()));
                    KeyValue keyValue = future.get().getKvs().get(0);
                    log.info("init property key={},value={}, property={}.", keyValue.getKey(), keyValue.getValue(), v.getClass());
                    inflateProperty(keyValue, v);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void inflateProperty(KeyValue keyValue, Object v) {
        if (v == null) {
            return;
        }
        if (propertyVersion.get(v) != null && keyValue.getModRevision() == propertyVersion.get(v)) {
            return;
        }
        Object o = yaml.loadAs(keyValue.getValue().toStringUtf8(), v.getClass());
        BeanUtils.copyProperties(o, v);
        propertyVersion.put(v, keyValue.getModRevision());
    }
}
