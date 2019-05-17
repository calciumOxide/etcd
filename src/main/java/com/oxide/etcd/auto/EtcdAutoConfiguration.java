package com.oxide.etcd.auto;


import com.oxide.etcd.EtcdConnection;
import com.oxide.etcd.annotation.Etcd;
import com.oxide.etcd.config.EtcdConfig;
import com.oxide.etcd.scanner.ClassPathEtcdScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(EtcdConfig.class)
@Import({EtcdConnection.class, EtcdAutoConfiguration.AutoConfiguredEtcdScannerRegistrar.class})
public class EtcdAutoConfiguration {

    @Autowired
    EtcdConfig etcdConfig;

    @PostConstruct
    public void init() {
        EtcdConnection.connect(etcdConfig);
        EtcdConnection.watch();
    }

    public static class AutoConfiguredEtcdScannerRegistrar implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware {

        private BeanFactory beanFactory;

        private ResourceLoader resourceLoader;


        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            log.debug("Searching for etcd annotated with @Ectd");

            ClassPathEtcdScanner scanner = new ClassPathEtcdScanner(registry);

            try {
                if (this.resourceLoader != null) {
                    scanner.setResourceLoader(this.resourceLoader);
                }

                List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
                if (log.isDebugEnabled()) {
                    for (String pkg : packages) {
                        log.debug("Using auto-configuration base package '{}'", pkg);
                    }
                }

                scanner.setAnnotationClass(Etcd.class);
                scanner.doScan(StringUtils.toStringArray(packages));
            } catch (IllegalStateException ex) {
                log.debug("Could not determine auto-configuration package, automatic ectd scanning disabled.", ex);
            }
        }

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }

        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }
    }


}
