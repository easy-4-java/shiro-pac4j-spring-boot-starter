package org.apache.shiro.spring.boot;

import io.buji.pac4j.subject.Pac4jSubjectFactory;
import org.apache.shiro.event.EventBus;
import org.apache.shiro.event.support.DefaultEventBus;
import org.apache.shiro.mgt.SubjectFactory;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.CookieRememberMeManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.junit.jupiter.api.Test;
import org.pac4j.core.config.Config;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ShiroPac4jServletConfigurationTest {

    @Test
    void pac4jAndShiroBizShareOneServletRegistration() {
        verifyServletRegistration(new WebApplicationContextRunner(), true);
    }

    @Test
    void anApplicationCanDisableShiroRememberMeWhileRetainingPac4jRegistration() {
        verifyServletRegistration(new WebApplicationContextRunner().withUserConfiguration(WithoutRememberMe.class), false);
    }

    private static void verifyServletRegistration(WebApplicationContextRunner runner, boolean rememberMeEnabled) {
        runner.withConfiguration(AutoConfigurations.of(ShiroBizAutoConfiguration.class,
                        ShiroBizWebAutoConfiguration.class, ShiroBizWebFilterConfiguration.class,
                        ShiroPac4jWebAutoConfiguration.class, ShiroPac4jWebFilterConfiguration.class))
                .withBean(EventBus.class, DefaultEventBus::new)
                .withBean(Realm.class, SimpleAccountRealm::new)
                .withBean(Config.class, Config::new)
                .withPropertyValues("shiro.enabled=true", "shiro.pac4j.enabled=true",
                        "shiro.static-security-manager-enabled=false",
                        "shiro.filter-chain-definition-map.[/callback]=pac4j-callback")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(SubjectFactory.class)).isInstanceOf(Pac4jSubjectFactory.class);
                    FilterRegistrationBean<?> outer = context.getBean("filterShiroFilterRegistrationBean",
                            FilterRegistrationBean.class);
                    assertThat(outer.isEnabled()).isTrue();
                    ShiroFilterFactoryBean factory = context.getBean("&shiroFilterFactoryBean", ShiroFilterFactoryBean.class);
                    DefaultWebSecurityManager securityManager = context.getBean(DefaultWebSecurityManager.class);
                    assertThat(factory.getSecurityManager()).isSameAs(securityManager);
                    if (rememberMeEnabled) {
                        assertThat(securityManager.getRememberMeManager()).isInstanceOf(CookieRememberMeManager.class);
                    } else {
                        assertThat(securityManager.getRememberMeManager()).isNull();
                    }
                    assertThat(factory.getFilters()).containsKeys("pac4j", "pac4j-user", "pac4j-logout", "pac4j-callback");
                    assertThat(factory.getFilterChainDefinitionMap()).containsEntry("/callback", "pac4j-callback");
                    assertThat(context.getBean("pac4j-callback", FilterRegistrationBean.class).isEnabled()).isFalse();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class WithoutRememberMe {

        @Bean
        SmartInitializingSingleton disableShiroRememberMe(DefaultWebSecurityManager securityManager) {
            return () -> securityManager.setRememberMeManager(null);
        }
    }
}
