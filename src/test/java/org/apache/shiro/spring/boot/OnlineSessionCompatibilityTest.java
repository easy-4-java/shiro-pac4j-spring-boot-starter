package org.apache.shiro.spring.boot;

import org.apache.shiro.biz.session.mgt.SimpleOnlineSessionFactory;
import org.apache.shiro.session.mgt.SimpleOnlineSession;
import org.apache.shiro.web.session.mgt.DefaultWebSessionContext;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class OnlineSessionCompatibilityTest {

    @Test
    void createsAnOnlineSessionForAnOrdinaryServletRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.setLocalPort(8080);
        request.addHeader("User-Agent", "online-session-test");
        DefaultWebSessionContext context = new DefaultWebSessionContext();
        context.setServletRequest(request);

        SimpleOnlineSession session = (SimpleOnlineSession) new SimpleOnlineSessionFactory().createSession(context);

        assertThat(session.getHost()).isEqualTo("127.0.0.1");
        assertThat(session.getUserAgent()).isEqualTo("online-session-test");
        assertThat(session.getSystemHost()).isEqualTo("127.0.0.1:8080");
    }
}
