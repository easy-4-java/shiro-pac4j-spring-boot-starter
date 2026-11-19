package org.apache.shiro.spring.boot.pac4j.cas.token;

import java.util.List;

import org.apache.shiro.biz.authc.token.LoginType;
import org.junit.jupiter.api.Test;
import org.pac4j.core.profile.CommonProfile;

import static org.assertj.core.api.Assertions.assertThat;

class Pac4jCasTokenTest {

    @Test
    void keepsTheCasCredentialsAndProfilesAccessibleToShiro() {
        var profile = new CommonProfile();
        profile.setId("fixture-user");
        var token = new Pac4jCasToken("fixture-initial-host", List.of(profile), true);
        token.setUsername("fixture-user");
        token.setTicket("fixture-ticket");
        token.setHost("fixture-host");

        assertThat(token.getPrincipal()).isEqualTo("fixture-user");
        assertThat(token.getCredentials()).isEqualTo("fixture-ticket");
        assertThat(token.getTicket()).isEqualTo("fixture-ticket");
        assertThat(token.getHost()).isEqualTo("fixture-host");
        assertThat(token.getLoginType()).isEqualTo(LoginType.CAS);
        assertThat(token.getProfiles()).containsExactly(profile);
        assertThat(token.isRememberMe()).isTrue();
    }
}
