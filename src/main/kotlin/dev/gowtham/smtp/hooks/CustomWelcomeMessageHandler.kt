package dev.gowtham.smtp.hooks

import com.google.inject.Inject
import dev.gowtham.smtp.utils.SMTPUtil
import org.apache.james.protocols.api.ProtocolSession
import org.apache.james.protocols.api.Response
import org.apache.james.protocols.smtp.SMTPSession
import org.apache.james.smtpserver.JamesWelcomeMessageHandler

class CustomWelcomeMessageHandler : JamesWelcomeMessageHandler() {

	@Inject
	private lateinit var smtpUtil: SMTPUtil

	override fun onConnect(session: SMTPSession): Response {
		// init connection parameters here
		smtpUtil.setSessionUUID(session as ProtocolSession)
		smtpUtil.initSessionTimer(session as ProtocolSession)
		return super.onConnect(session)
	}
}

