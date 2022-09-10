package dev.gowtham.smtp.hooks

import org.apache.james.core.Username
import org.apache.james.protocols.api.OidcSASLConfiguration
import org.apache.james.protocols.smtp.SMTPSession
import org.apache.james.protocols.smtp.hook.AuthHook
import org.apache.james.protocols.smtp.hook.HookResult

class CustomAuthHook : AuthHook {
	override fun doAuth(session: SMTPSession, username: Username, password: String): HookResult {
		return HookResult.DECLINED
	}

	override fun doSasl(
		session: SMTPSession?,
		saslConfiguration: OidcSASLConfiguration?,
		initialResponse: String?
	): HookResult {
		return HookResult.DECLINED
	}
}
