package org.apache.shiro.spring.boot.pac4j;

import java.util.Collections;
import java.util.List;

import io.buji.pac4j.subject.Pac4jPrincipal;
import io.buji.pac4j.token.Pac4jToken;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.biz.authc.token.LoginType;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.spring.boot.pac4j.cas.token.Pac4jCasToken;
import org.apache.shiro.spring.boot.pac4j.ext.filter.Pac4jUserFilter;
import org.apache.shiro.spring.boot.pac4j.ext.realm.Pac4jAuthorizingRealm;
import org.apache.shiro.spring.boot.pac4j.jwt.Pac4jJwtRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.Pac4jConstants;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class Pac4jIdentityIntegrationTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void authenticatesProfilesAndKeepsAuthorizationCachesSeparatedByAccount(boolean jwt) {
        AuthorizingRealm realm = jwt ? new Pac4jJwtRealm() : new Pac4jAuthorizingRealm();
        realm.setCacheManager(new MemoryConstrainedCacheManager());
        realm.setAuthenticationCachingEnabled(true);
        realm.setAuthorizationCachingEnabled(true);
        realm.init();

        Pac4jToken auditor = profileToken("auditor", "reports:read");
        Pac4jToken editor = profileToken("editor", "reports:write");
        AuthenticationInfo auditorInfo = realm.getAuthenticationInfo(auditor);
        AuthenticationInfo editorInfo = realm.getAuthenticationInfo(editor);
        assertThat(realm.getAuthenticationInfo(auditor)).isSameAs(auditorInfo);
        assertThat(realm.getAuthenticationInfo(editor)).isSameAs(editorInfo);

        PrincipalCollection auditorPrincipals = auditorInfo.getPrincipals();
        PrincipalCollection editorPrincipals = editorInfo.getPrincipals();
        assertThat(((Pac4jPrincipal) auditorPrincipals.getPrimaryPrincipal()).getProfile().getId()).isEqualTo("auditor");
        assertThat(realm.isPermitted(auditorPrincipals, "reports:read")).isTrue();
        assertThat(realm.isPermitted(editorPrincipals, "reports:write")).isTrue();
        assertThat(realm.isPermitted(editorPrincipals, "reports:read")).isFalse();
        assertThat(realm.getAuthorizationCache().keys()).containsExactlyInAnyOrder("auditor", "editor");
    }

    @Test
    void casTokenExposesTheAuthenticatedProfileAndTicketFields() {
        List<CommonProfile> profiles = profileToken("auditor", "reports:read").getProfiles();
        Pac4jCasToken token = new Pac4jCasToken("localhost", profiles, true);
        assertThat(token.getHost()).isEqualTo("localhost");
        assertThat(token.getProfiles()).isEqualTo(profiles);
        assertThat(token.isRememberMe()).isTrue();

        token.setUsername("auditor");
        token.setTicket("test-ticket");
        token.setHost("127.0.0.1");

        assertThat(token.getPrincipal()).isEqualTo("auditor");
        assertThat(token.getCredentials()).isEqualTo(token.getTicket()).isEqualTo("test-ticket");
        assertThat(token.getHost()).isEqualTo("127.0.0.1");
        assertThat(token.getLoginType()).isEqualTo(LoginType.CAS);
    }

    @Test
    void savesTheRequestedPathForPac4jAndShiroBeforeLogin() throws Exception {
        Subject subject = new Subject.Builder(new DefaultSecurityManager()).buildSubject();
        Pac4jUserFilter filter = new Pac4jUserFilter();
        filter.setLoginUrl("/login");
        filter.processPathConfig("/reports/**", null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/reports/daily");
        request.setServletPath("/reports/daily");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        subject.associateWith(() -> {
            filter.doFilter(request, response, chain);
            assertThat(subject.getSession().getAttribute(Pac4jConstants.REQUESTED_URL)).isEqualTo("/reports/daily");
            assertThat(WebUtils.getSavedRequest(request).getRequestURI()).isEqualTo("/reports/daily");
            return null;
        }).call();

        assertThat(response.getRedirectedUrl()).isEqualTo("/login");
        assertThat(chain.getRequest()).isNull();
    }

    private Pac4jToken profileToken(String id, String permission) {
        CommonProfile profile = new CommonProfile();
        profile.setId(id);
        profile.addPermission(permission);
        return new Pac4jToken(Collections.singletonList(profile), false);
    }
}
