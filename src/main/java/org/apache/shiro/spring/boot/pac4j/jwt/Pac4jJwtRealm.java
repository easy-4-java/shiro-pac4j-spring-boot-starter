package org.apache.shiro.spring.boot.pac4j.jwt;

import io.buji.pac4j.token.Pac4jToken;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.spring.boot.pac4j.ext.realm.Pac4jAuthorizingRealm;
import org.pac4j.core.profile.UserProfile;

import java.util.List;
import java.util.Objects;

/**
 * 登录后 Principal 为 Pac4jPrincipal对象,获取cas传递回来的username,
 * 通过:String username = pac4jPrincipal.getProfile().getId();
 * 如果开启了缓存,应重写权限缓存以及认证缓存的key值,在AuthorizingRealm中的getAuthorizationCacheKey以及getAuthenticationCacheKey,推荐使用username来作为缓存key
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public class Pac4jJwtRealm extends Pac4jAuthorizingRealm {

	@Override
	protected Object getAuthenticationCacheKey(AuthenticationToken token) {
		if (token instanceof Pac4jToken) {

			final Pac4jToken pac4jToken = (Pac4jToken) token;
			final List<UserProfile> profiles = pac4jToken.getProfiles();
			// 会话 Principal 的哈希含随机 ID，缓存键保持原有的 profile 列表哈希。
			return String.valueOf(Objects.hashCode(profiles));
		}
		return super.getAuthenticationCacheKey(token);
	}

}
