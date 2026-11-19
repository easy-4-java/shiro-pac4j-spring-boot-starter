# Issue #3 的 Remember Me 配置验证

本分支验证 Spring Boot 2.7 在 Java 8 下的运行行为。Apache Shiro 将 Remember Me 已启用列为 CVE-2026-56130 的影响条件。对于无需 Shiro Cookie 持久登录的应用，可在自己的 Spring 配置类中注册下面的 Bean。[Shiro 公告](https://shiro.apache.org/security-reports.html#cve-2026-56130)

```java
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ShiroRememberMeConfiguration {

    @Bean
    SmartInitializingSingleton disableShiroRememberMe(DefaultWebSecurityManager securityManager) {
        return () -> securityManager.setRememberMeManager(null);
    }
}
```

集成测试确认默认 Web 安全管理器装配 CookieRememberMeManager，显式配置后其 Remember Me 管理器为空。两条启动路径均验证实际 Shiro 过滤器工厂引用同一个安全管理器、Pac4j 回调过滤器只注册在 Shiro 过滤链内、回调映射保持可用。JDK 8 `clean verify` 执行 8 项测试，零失败、零错误、零跳过。

此验证覆盖 Starter 的配置和过滤器注册。应用负责验证自身的认证方式及自定义安全管理器。依赖升级与 Issue #3 的其余公告仍需各自验收。[Issue #3](https://github.com/easy-4-java/shiro-pac4j-spring-boot-starter/issues/3)
