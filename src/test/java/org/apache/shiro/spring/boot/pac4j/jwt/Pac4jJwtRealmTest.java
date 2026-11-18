package org.apache.shiro.spring.boot.pac4j.jwt;

import java.util.List;

import io.buji.pac4j.subject.Pac4jPrincipal;
import io.buji.pac4j.token.Pac4jToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;

import static org.assertj.core.api.Assertions.assertThat;

class Pac4jJwtRealmTest {

    @Test
    void preservesTheJwtRealmCacheKeys() {
        var realm = new Pac4jJwtRealm();
        var profile = new CommonProfile();
        profile.setId("fixture-user");
        var profiles = List.<UserProfile>of(profile);
        var principal = new Pac4jPrincipal(profiles);

        assertThat(realm.getAuthorizationCacheKey(new SimplePrincipalCollection(principal, realm.getName())))
                .isEqualTo("fixture-user");
        assertThat(realm.getAuthenticationCacheKey(new Pac4jToken(profiles, false)))
                .isEqualTo(String.valueOf(principal.hashCode()));
        assertThat(realm.getAuthenticationCacheKey(new UsernamePasswordToken("other-account", "test-input")))
                .isEqualTo("other-account");
    }
}
