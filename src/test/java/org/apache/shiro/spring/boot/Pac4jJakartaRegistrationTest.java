package org.apache.shiro.spring.boot;

import java.util.List;

import io.buji.pac4j.realm.Pac4jRealm;
import io.buji.pac4j.subject.Pac4jPrincipal;
import io.buji.pac4j.subject.Pac4jSubjectFactory;
import jakarta.servlet.Filter;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SubjectFactory;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.junit.jupiter.api.Test;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.core.engine.CallbackLogic;
import org.pac4j.core.engine.LogoutLogic;
import org.pac4j.core.engine.SecurityLogic;
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
import static org.mockito.Mockito.when;

class Pac4jJakartaRegistrationTest {

    @Test
    void registersThePac4jFiltersOnlyInTheShiroChain() {
        new WebApplicationContextRunner()
                .withUserConfiguration(Application.class)
                .withBean(Config.class, Pac4jJakartaRegistrationTest::fixtureConfig)
                .withBean(Pac4jRealm.class, Pac4jRealm::new)
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
    void startsWithThePac4jAutoConfiguration() {
        new WebApplicationContextRunner()
                .withUserConfiguration(Application.class)
                .withBean(SimpleAccountRealm.class, SimpleAccountRealm::new)
                .withBean(Client.class, () -> {
                    Client client = mock(Client.class);
                    when(client.getName()).thenReturn("fixture");
                    return client;
                })
                .withPropertyValues(
                        "pac4j.enabled=true",
                        "pac4j.clients=fixture",
                        "shiro.pac4j.enabled=true",
                        "shiro.cache.type=memory")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(Config.class).getClients().getDefaultSecurityClients()).isEqualTo("fixture");
                });
    }

    private static Config fixtureConfig() {
        var config = new Config();
        config.setSecurityLogic((SecurityLogic)
                (context, session, settings, granted, response, clients, authorizers, matchers, parameters) -> {
                    assertThat(context.getPath()).isEqualTo("/protected/account");
                    assertThat(SecurityUtils.getSubject().getPrincipal())
                            .isInstanceOfSatisfying(Pac4jPrincipal.class,
                                    principal -> assertThat(principal.getProfile().getId()).isEqualTo("fixture-user"));
                    ((JEEContext) context).getNativeResponse().setStatus(202);
                    return null;
                });
        config.setCallbackLogic((CallbackLogic)
                (context, session, settings, response, defaultUrl, renewSession, defaultClient) -> {
                    assertThat(context.getPath()).isEqualTo("/callback");
                    var profile = new CommonProfile();
                    profile.setId("fixture-user");
                    settings.getProfileManagerFactory().apply(context, session).save(true, profile, false);
                    assertThat(SecurityUtils.getSubject().isAuthenticated()).isTrue();
                    ((JEEContext) context).getNativeResponse().setStatus(203);
                    return null;
                });
        config.setLogoutLogic((LogoutLogic)
                (context, session, settings, response, defaultUrl, pattern, local, destroySession, central) -> {
                    assertThat(context.getPath()).isEqualTo("/logout");
                    settings.getProfileManagerFactory().apply(context, session).removeProfiles();
                    assertThat(SecurityUtils.getSubject().isAuthenticated()).isFalse();
                    ((JEEContext) context).getNativeResponse().setStatus(204);
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
