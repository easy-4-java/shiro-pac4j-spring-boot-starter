# Issue #3 的 Spring Boot 3.5 安全依赖评审

基线为当前 Spring Boot 3.5.16 / Spring Framework 6.2.19 / Shiro 2.2.1，JDK 17。上游与叶子模块均用实际源码构建。

| 仓库 | 验证基线 |
| --- | --- |
| shiro-pac4j-spring-boot-starter | `43f86ec2e58c3bd1513078b57475150e4c1db50a` |
| shiro-extension-spring-boot-starter | `d346ac8b06aed6321de37ba0e01a0c6ccd9d97fd` |
| shiro-extension | `3f837a979b5a3ed2bb52bf7596525b20035d81b3` |

## 在线会话兼容性

在叶子模块中引入 Jakarta 版 Shiro 3.0.2 Web starter，排除上游传递的 Shiro 2.2.1 以及默认 Tomcat，原有 5 项测试通过。新增的 `OnlineSessionCompatibilityTest` 使用普通 Servlet 请求调用 `SimpleOnlineSessionFactory`，校验会话中的地址、User-Agent 和系统地址。原扩展二进制调用 Shiro 3 已移除的 `SimpleSession#setHost`，触发 `NoSuchMethodError`。

临时迁移方案在扩展会话工厂中用 `new SimpleOnlineSession(host)` 初始化地址，保留空上下文处理与请求元数据。上游 starter 的端点测试也用相同构造方法。上游 starter 使用单一的无 classifier 的 Shiro 3.0.2 Web starter，排除传递的 Shiro 2.x 核心依赖。旧的 `shiro-ehcache`、`shiro-hazelcast` 仍以 2.2.1 的 `provided` 坐标用于候选构建，Maven Central 没有这两个组件的 3.0.2 坐标。

## 多 Realm 认证和历史会话

只迁移会话工厂的候选组合可以读取由 Shiro 2.2.1 写出的本地会话样本。新增的 `MultiRealmCompatibilityTest` 在原依赖下能通过。在候选组合中，同一账号经两个 Realm 认证仍被 `AtLeastTwoAuthenticatorStrategy` 拒绝。

扩展源码直接在 Shiro 3.0.2 下编译时，编译器提示 `RealmAuthenticationInfo#merge` 的返回类型已由 `void` 变为 `AuthenticationInfo`。临时迁移复用扩展 3.x 的聚合实现和 PrincipalCollection 测试适配。此改动保留按成功 Realm 数量判断的认证规则。[Shiro 扩展 3.x 聚合实现](https://github.com/easy-4-java/shiro-extension/blob/865d30d924af1b1f56a49bc80c0b3a190e8ef6ca/shiro-extension-core/src/main/java/org/apache/shiro/biz/authc/pam/RealmAuthenticationInfo.java)

| JDK 17 验证 | 结果 |
| --- | --- |
| Pac4j starter，原始 Shiro 2.2.1 依赖及两个新增回归测试 | 7 项通过，0 跳过 |
| Pac4j starter，仅替换 Shiro 3.0.2 并运行新增会话测试 | 1 项发生链接错误 |
| Shiro 扩展的 core + spring 模块，候选工厂在原始 Shiro 2.2.1 下 | 142 项中 119 项通过，23 项因缺少外部缓存服务等条件跳过 |
| 上游 Shiro Biz starter，候选扩展和 Shiro 3.0.2 | 24 项通过，0 跳过 |
| Pac4j starter，只迁移会话工厂并运行双 Realm 认证测试 | 认证被拒绝 |
| Shiro 扩展 core + spring 模块，迁移会话工厂和聚合签名并使用 Shiro 3.0.2 编译 | 142 项中 119 项通过，23 项跳过 |
| 上游 Shiro Biz starter，以 Shiro 3.0.2 重新构建扩展后 | 24 项通过，0 跳过 |
| Pac4j starter，完整候选组合 | 7 项通过，0 跳过 |
| 完整候选组合读取原 Shiro 2.2.1 生成的 JDK 序列化会话样本 | 会话 ID、自定义属性、客户端地址、User-Agent 和系统地址均保留 |

## 安全依赖的剩余项

候选 Pac4j starter 的 Maven runtime 树包含 74 个唯一 Maven 坐标，使用 OSV 查询这些解析后的版本。Shiro 的 `GHSA-m8g6-vrr2-x7ff`（`CVE-2026-56130`）已从结果中消失，`spring-webmvc:6.2.19` 仍命中两条公告。Spring 将 6.2.20 的两个修复均列为企业支持版本，开源修复版本为 7.0.9。[Shiro 公告](https://shiro.apache.org/security-reports.html#cve-2026-56130) · [CVE-2026-47884](https://spring.io/security/cve-2026-47884/) · [CVE-2026-47890](https://spring.io/security/cve-2026-47890/)

Shiro 3.0.2 的公开父 POM 使用 Spring Boot 4.1.2 和 Spring Framework 7.0.10。在 Boot 3.5 上采用候选方案还需确认跨版本支持范围、按启用的共享缓存提供者验证历史会话恢复和实际运行时行为。此次功能分支提交会话和多 Realm 回归测试以及评审证据。候选生产代码保留在临时验证副本，Issue #3 的 Boot 3.5 安全验收仍开放。[Shiro 3.0.2 父 POM](https://repo.maven.apache.org/maven2/org/apache/shiro/shiro-root/3.0.2/shiro-root-3.0.2.pom) · [Issue #3](https://github.com/easy-4-java/shiro-pac4j-spring-boot-starter/issues/3)

## 不使用 Remember Me 的应用

Boot 3.5 的 Shiro Biz 自动配置会创建 `CookieRememberMeManager`。Shiro 将 Remember Me 已启用列为 CVE-2026-56130 的影响条件。确认应用无需 Cookie 持久登录后，可在应用配置中注册下面的 Bean。它会在 Spring 完成单例初始化时移除默认 Shiro Web 安全管理器上的 Remember Me 管理器。[Shiro 公告](https://shiro.apache.org/security-reports.html#cve-2026-56130)

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

新增回归在默认启动路径观察到 `CookieRememberMeManager`，在显式配置的路径观察到空管理器。两种配置下，经过签名和加密的 JWT 均通过真实 Shiro 过滤链完成认证，无认证凭据的请求均返回 401。JDK 17 `clean verify` 执行 8 项，零失败、零错误、零跳过，达到原覆盖率门禁。早期 BeanPostProcessor 方案在实际自动配置中未移除管理器。

此配置影响 Shiro 的 Cookie 持久登录。自定义安全管理器和独立配置的其他认证路径应在各自应用中验证。Shiro 与 Spring 依赖的升级验收仍按本记录前文执行。
