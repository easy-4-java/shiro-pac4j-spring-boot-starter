package org.apache.shiro.spring.boot;

import java.util.List;

import org.apache.shiro.spring.boot.pac4j.ext.realm.Pac4jAuthorizingRealm;
import io.buji.pac4j.subject.Pac4jPrincipal;
import io.buji.pac4j.subject.Pac4jSubjectFactory;
import jakarta.servlet.Filter;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SubjectFactory;
import org.apache.shiro.spring.boot.pac4j.jwt.Pac4jJwtRealm;
import org.junit.jupiter.api.Test;
import org.pac4j.core.ext.authentication.AuthenticatingFailureCounter;
import org.pac4j.core.ext.authentication.captcha.CaptchaResolver;
import org.pac4j.core.config.Config;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.LogoutLogic;
import org.pac4j.core.engine.SecurityLogic;
import org.pac4j.jwt.config.encryption.EncryptionConfiguration;
import org.pac4j.jwt.config.signature.SignatureConfiguration;
import org.pac4j.jwt.profile.JwtGenerator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Pac4jJakartaRegistrationTest {

    @Test
    void registersThePac4jFiltersOnlyInTheShiroChain() {
        new WebApplicationContextRunner()
                .withUserConfiguration(Application.class)
                .withBean(Config.class, Pac4jJakartaRegistrationTest::fixtureConfig)
                .withBean(Pac4jAuthorizingRealm.class, Pac4jAuthorizingRealm::new)
                .withPropertyValues(
                        "shiro.pac4j.enabled=true",
                        "shiro.enabled=true",
                        "shiro.cache.type=memory",
                        "shiro.filter-chain-definition-map.[/protected/**]=pac4j",
                        "shiro.filter-chain-definition-map.[/saved/**]=pac4j-user",
                        "pac4j.login-url=/sign-in",
                        "shiro.filter-chain-definition-map.[/callback]=pac4j-callback",
                        "shiro.filter-chain-definition-map.[/logout]=pac4j-logout")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(SubjectFactory.class)).isInstanceOf(Pac4jSubjectFactory.class);
                    for (String name : List.of("pac4j", "pac4j-user", "pac4j-callback", "pac4j-logout")) {
                        var registration = context.getBean(name, FilterRegistrationBean.class);
                        assertThat(registration.isEnabled()).isFalse();
                        assertThat(registration.getFilter()).isInstanceOf(Filter.class);
                    }
                    Filter dispatch = context.getBean("filterShiroFilterRegistrationBean", FilterRegistrationBean.class)
                            .getFilter();
                    var session = new MockHttpSession(context.getServletContext());
                    assertThat(request(dispatch, "/saved/account", session)).isEqualTo(302);
                    assertThat(session.getAttribute(Pac4jConstants.REQUESTED_URL)).isEqualTo("/saved/account");
                    assertThat(request(dispatch, "/callback", session)).isEqualTo(203);
                    assertThat(request(dispatch, "/protected/account", session)).isEqualTo(202);
                    assertThat(request(dispatch, "/logout", session)).isEqualTo(204);
                });
    }

    @Test
    void authenticatesASignedAndEncryptedJwtThroughTheShiroChain() {
        new WebApplicationContextRunner()
                .withUserConfiguration(Application.class)
                .withBean(Pac4jJwtRealm.class, Pac4jJwtRealm::new)
                .withBean(CaptchaResolver.class, () -> mock(CaptchaResolver.class))
                .withBean(AuthenticatingFailureCounter.class, () -> mock(AuthenticatingFailureCounter.class))
                .withPropertyValues(
                        "pac4j.enabled=true",
                        "pac4j.clients=jwt-header",
                        "pac4j.jwt.enabled=true",
                        "pac4j.jwt.sign-secret=00000000000000000000000000000000",
                        "pac4j.jwt.encrypt-secret=11111111111111111111111111111111",
                        "pac4j.jwt.authorization-header-name=X-Fixture-Token",
                        "pac4j.jwt.authorization-cookie-name=fixture-cookie",
                        "pac4j.jwt.authorization-param-name=fixture-query",
                        "pac4j.jwt.login-url=/sign-in",
                        "pac4j.jwt.callback-url=https://app.example.invalid/callback",
                        "pac4j.jwt.default-client-name=fixture-form",
                        "shiro.pac4j.enabled=true",
                        "shiro.cache.type=memory",
                        "shiro.filter-chain-definition-map.[/jwt]=pac4j")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var profile = new CommonProfile();
                    profile.setId("jwt-fixture");
                    var generator = new JwtGenerator(context.getBean(SignatureConfiguration.class),
                            context.getBean(EncryptionConfiguration.class));
                    Filter dispatch = context.getBean("filterShiroFilterRegistrationBean", FilterRegistrationBean.class)
                            .getFilter();
                    var request = new MockHttpServletRequest("GET", "/jwt");
                    request.setServletPath("/jwt");
                    request.addHeader("X-Fixture-Token", generator.generate(profile));
                    var response = new MockHttpServletResponse();
                    dispatch.doFilter(request, response, (servletRequest, servletResponse) -> {
                        assertThat(SecurityUtils.getSubject().isAuthenticated()).isTrue();
                        assertThat(SecurityUtils.getSubject().getPrincipal())
                                .isInstanceOfSatisfying(Pac4jPrincipal.class,
                                        principal -> assertThat(principal.getProfile().getId()).isEqualTo("jwt-fixture"));
                        response.setStatus(204);
                    });
                    assertThat(response.getStatus()).isEqualTo(204);
                    assertThat(request(dispatch, "/jwt", new MockHttpSession(context.getServletContext())))
                            .isEqualTo(401);
                });
    }

    private static Config fixtureConfig() {
        var config = new Config();
        config.setSecurityLogic((SecurityLogic)
                (settings, granted, clients, authorizers, matchers, parameters) -> {
                    var context = (JEEContext) settings.getWebContextFactory().newContext(parameters);
                    assertThat(context.getPath()).isEqualTo("/protected/account");
                    assertThat(SecurityUtils.getSubject().getPrincipal())
                            .isInstanceOfSatisfying(Pac4jPrincipal.class,
                                    principal -> assertThat(principal.getProfile().getId()).isEqualTo("fixture-user"));
                    context.getNativeResponse().setStatus(202);
                    return null;
                });
        config.setCallbackLogic((CallbackLogic)
                (settings, defaultUrl, renewSession, defaultClient, parameters) -> {
                    var context = (JEEContext) settings.getWebContextFactory().newContext(parameters);
                    assertThat(context.getPath()).isEqualTo("/callback");
                    var profile = new CommonProfile();
                    profile.setId("fixture-user");
                    settings.getProfileManagerFactory().apply(context, settings.getSessionStoreFactory().newSessionStore(parameters)).save(true, profile, false);
                    assertThat(SecurityUtils.getSubject().isAuthenticated()).isTrue();
                    context.getNativeResponse().setStatus(203);
                    return null;
                });
        config.setLogoutLogic((LogoutLogic)
                (settings, defaultUrl, pattern, local, destroySession, central, parameters) -> {
                    var context = (JEEContext) settings.getWebContextFactory().newContext(parameters);
                    assertThat(context.getPath()).isEqualTo("/logout");
                    settings.getProfileManagerFactory().apply(context, settings.getSessionStoreFactory().newSessionStore(parameters)).removeProfiles();
                    assertThat(SecurityUtils.getSubject().isAuthenticated()).isFalse();
                    context.getNativeResponse().setStatus(204);
                    return null;
                });
        return config;
    }

    private static int request(Filter dispatch, String path, MockHttpSession session) throws Exception {
        var request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        request.setSession(session);
        var response = new MockHttpServletResponse();
        dispatch.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Application {
    }
}
