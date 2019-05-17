package com.oxide.etcd.factory;

import com.oxide.etcd.EtcdConnection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.FactoryBean;

import static org.springframework.util.Assert.notNull;

@Data
@ToString
public class EtcdFactoryBean<T> implements FactoryBean<T> {

    private Class<T> etcdProperty;

    private EtcdFactoryBean() {

    }

    private EtcdFactoryBean(Class<T> etcdProperty) {
        this.etcdProperty = etcdProperty;
    }

    @Override
    public T getObject() {
        notNull(this.etcdProperty, "Property 'etcdProperty' is required");
        return EtcdConnection.getEtcdProperty(etcdProperty);
    }

    @Override
    public Class<?> getObjectType() {
        return etcdProperty;
    }
}
