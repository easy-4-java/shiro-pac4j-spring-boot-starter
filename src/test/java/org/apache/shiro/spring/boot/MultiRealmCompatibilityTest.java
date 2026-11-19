package org.apache.shiro.spring.boot;

import java.util.List;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.biz.authc.pam.AtLeastTwoAuthenticatorStrategy;
import org.apache.shiro.biz.authc.pam.DefaultModularRealmAuthenticator;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultiRealmCompatibilityTest {

    @Test
    void authenticatesWhenBothRealmsAcceptTheSameLocalTestAccount() {
        SimpleAccountRealm first = new SimpleAccountRealm("first");
        first.addAccount("alice", "fixture-password");
        SimpleAccountRealm second = new SimpleAccountRealm("second");
        second.addAccount("alice", "fixture-password");
        DefaultModularRealmAuthenticator authenticator = new DefaultModularRealmAuthenticator();
        authenticator.setRealms(List.of(first, second));
        authenticator.setAuthenticationStrategy(new AtLeastTwoAuthenticatorStrategy());

        var authenticated = authenticator.authenticate(new UsernamePasswordToken("alice", "fixture-password"));

        assertThat(authenticated.getPrincipals().getRealmNames()).containsExactlyInAnyOrder("first", "second");
    }
}
