<a id="readme-top"></a>

<div align="center">

# shiro-pac4j-spring-boot-starter

**Spring Boot Starter for shiro-pac4j**

[![Maven Central](https://img.shields.io/maven-central/v/io.github.easy4j/shiro-pac4j-spring-boot-starter)](https://github.com/easy-4-java/shiro-pac4j-spring-boot-starter)
[![Java](https://img.shields.io/badge/Java-17-orange)](#3-requirements-and-compatibility)
[![License](https://img.shields.io/badge/license-Apache-2.0-green)](https://www.apache.org/licenses/LICENSE-2.0)

[简体中文](./README.zh-CN.md) | [English](./README.md)

[Positioning](#1-positioning) · [Capabilities](#2-core-capabilities) ·
[Dependency](#5-dependency) · [Quick Start](#6-quick-start) ·
[Configuration](#7-configuration-reference) · [Versions](#9-version-lines-and-compatibility) ·
[Build](#10-build-and-test) · [License](#12-license)

</div>

---

> **Current Version**：`2.0.1-SNAPSHOT`<br>
> **JDK Baseline**：`8`<br>
> **Group ID**：`io.github.easy4j`<br>
> **Artifact ID**：`shiro-pac4j-spring-boot-starter`<br>
> **License**：Apache License 2.0<br>

## 1. Positioning

**shiro-pac4j-spring-boot-starter** is a Spring Boot starter that integrates **shiro-pac4j** for applications using shiro-pac4j. It provides auto-configuration, property binding, and ready-to-use beans so that applications can consume shiro-pac4j capabilities with minimal setup.

| Dimension | Description |
|---|---|
| Type | Spring Boot Starter |
| Consumers | Spring Boot applications using shiro-pac4j |
| Core Capabilities | auto-configuration, property binding, ready-to-use beans for shiro-pac4j |
| JDK | `8` |
| Coordinates | `io.github.easy4j:shiro-pac4j-spring-boot-starter:2.0.1-SNAPSHOT` |
| Config Prefix | `shiro.pac4j` |

## 2. Core Capabilities

| Capability | Status | Description |
|---|:---:|---|
| Auto-configuration | ✅ Stable | Registers shiro-pac4j beans automatically |
| Property Binding | ✅ Stable | Binds `shiro.pac4j.*` to `ShiroPac4jProperties` |
| Ready-to-use beans | ✅ Stable | Auto-registered via ShiroPac4jWebAutoConfiguration |

## 3. Requirements and Compatibility

| Dependency | Minimum | Evidence |
|---|---:|---|
| JDK | `8` | `pom.xml` |
| Spring Boot | `2.6.15` | `pom.xml` parent |
| Maven | `3.6+` | Maven Enforcer |

## 4. Auto-configuration

The starter auto-configures the following beans:

| Bean | Condition | Missing Behavior |
|---|---|---|
| `Object` | classpath + property | not created |

Auto-configuration registration:

- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Spring Boot 2.7+ / 3.x / 4.x)
- `META-INF/spring.factories` (Spring Boot 2.x legacy)

## 5. Dependency

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>shiro-pac4j-spring-boot-starter</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

No additional easy4j component dependencies.

### Boot 2.6 application dependency versions

The `main` line uses Spring Boot 2.6.15 and Java 8. The application POM controls dependency management for its runtime tree. In an application that inherits `spring-boot-starter-parent:2.6.15`, merge these entries into the existing `properties` and `dependencyManagement` sections to select the versions tested with this starter.

```xml
<properties>
    <spring-framework.version>5.3.39</spring-framework.version>
    <jackson-bom.version>2.21.8</jackson-bom.version>
    <snakeyaml.version>2.7</snakeyaml.version>
    <commons-lang3.version>3.22.0</commons-lang3.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>commons-beanutils</groupId>
            <artifactId>commons-beanutils</artifactId>
            <version>1.11.0</version>
        </dependency>
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>33.7.2-jre</version>
        </dependency>
        <dependency>
            <groupId>org.pac4j</groupId>
            <artifactId>pac4j-core</artifactId>
            <version>4.5.10</version>
        </dependency>
        <dependency>
            <groupId>org.pac4j</groupId>
            <artifactId>pac4j-config</artifactId>
            <version>4.5.10</version>
        </dependency>
        <dependency>
            <groupId>org.pac4j</groupId>
            <artifactId>pac4j-http</artifactId>
            <version>4.5.10</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Check the effective dependency tree from the application directory. Confirm that the runtime tree resolves Pac4j Core, Config, and HTTP to the same version. Continue scanning the application runtime for advisories that remain on this maintenance line.

```bash
mvn dependency:tree -Dscope=runtime
```

### CSRF advisory on the Java 8 line

The resolved `pac4j-core:4.5.10` artifact is still reported by OSV under CVE-2026-40458 because the advisory database range includes versions below 5.7.10. The Pac4j maintainer's CSRF announcement lists the 5.x and 6.x lines. The published 4.5.10 JAR matches the Maven Central checksum, and its built-in `CsrfAuthorizer` compares the request token with the full session token using `String.equals`. This verification covers the built-in authorizer in that artifact. Applications using a different artifact or a custom authorizer have their own verification scope. The dependency advisory stays visible in scans. [Pac4j announcement](https://www.pac4j.org/blog/security-advisory-pac4j-core-and-ldap.html) · [4.5.10 source](https://github.com/pac4j/pac4j/blob/pac4j-4.5.10/pac4j-core/src/main/java/org/pac4j/core/authorization/authorizer/CsrfAuthorizer.java) · [GitHub advisory](https://github.com/advisories/GHSA-xw5c-jc7x-gf75)

## 6. Quick Start

### 6.1 Add dependency

Add the dependency above to your `pom.xml`.

### 6.2 Configure

```yaml
shiro.pac4j:
  enabled: true
```

### 6.3 Use the bean

```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

Then inject the auto-configured bean in your code:

```java
@Autowired
private Object bean;
```

## 7. Configuration Reference

### 7.1 Config Prefix

`shiro.pac4j`

### 7.2 Configuration Items

| Property | Type | Default | Required | Description | Sensitive |
|---|---|---|:---:|---|:---:|
| `shiro.pac4j.enabled` | boolean | `true` | No | Enable the starter | No |
<!-- additional properties below -->

## 8. Version Lines and Compatibility

| Branch | JDK | Spring Boot | Component Version | Status |
|---|---:|---:|---|:---:|
| `2.3.x` / `2.7.x` | `8+` | 2.3.x / 2.7.x | `1.0.x` | Maintenance |
| `3.0.x` ~ `3.5.x` | `17` | 3.x | `2.0.x` | Maintenance |
| `4.0.x` / `4.1.x` | `17+` | 4.x | `3.0.x` | Active |

## 9. Build and Test

```bash
mvn clean verify
mvn -pl shiro-pac4j-spring-boot-starter -am test
```

## 10. Troubleshooting

| Symptom | Diagnosis | Resolution |
|---|---|---|
| Bean not created | Check auto-configuration report | Verify `shiro.pac4j.enabled=true` and classpath |
| `ClassNotFoundException` | Missing dependency | Add the required module |
| Version conflict | `mvn dependency:tree` | Use BOM for version alignment |

## 11. Contribution

1. Fork the repository.
2. Create a feature branch.
3. Run `mvn clean verify` before submitting.
4. Submit a pull request.

## 12. License

This project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

<div align="center">

[Back to top](#readme-top) · [Issues](https://github.com/easy-4-java/shiro-pac4j-spring-boot-starter/issues) · [Repository](https://github.com/easy-4-java/shiro-pac4j-spring-boot-starter)

</div>
