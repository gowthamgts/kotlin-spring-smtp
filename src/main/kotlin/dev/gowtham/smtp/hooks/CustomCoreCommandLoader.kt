package dev.gowtham.smtp.hooks

import org.apache.james.protocols.api.handler.CommandDispatcher
import org.apache.james.protocols.lib.handler.HandlersPackage
import org.apache.james.protocols.smtp.core.*
import org.apache.james.protocols.smtp.core.esmtp.AuthCmdHandler
import org.apache.james.protocols.smtp.core.esmtp.EhloCmdHandler
import org.apache.james.protocols.smtp.core.esmtp.MailSizeEsmtpExtension
import org.apache.james.protocols.smtp.core.esmtp.StartTlsCmdHandler
import org.apache.james.smtpserver.*
import java.util.*
import java.util.stream.Stream

class CustomCoreCommandLoader : HandlersPackage {
	private val handlers: LinkedList<String> = LinkedList()

	init {
		Stream.of(
			CustomWelcomeMessageHandler::class.java,
			CommandDispatcher::class.java,
			AuthCmdHandler::class.java,
			JamesDataCmdHandler::class.java,
			EhloCmdHandler::class.java,
			ExpnCmdHandler::class.java,
			HeloCmdHandler::class.java,
			HelpCmdHandler::class.java,
			JamesMailCmdHandler::class.java,
			NoopCmdHandler::class.java,
			QuitCmdHandler::class.java,
			RsetCmdHandler::class.java,
			VrfyCmdHandler::class.java,
			MailSizeEsmtpExtension::class.java,
			CustomAuthHook::class.java,
			AuthRequiredToRelayRcptHook::class.java,
			SenderAuthIdentifyVerificationRcptHook::class.java,
			PostmasterAbuseRcptHook::class.java,
			ReceivedDataLineFilter::class.java,
			DataLineJamesMessageHookHandler::class.java,
			StartTlsCmdHandler::class.java,
			AddDefaultAttributesMessageHook::class.java,
			UnknownCmdHandler::class.java,
		).map { obj -> obj.name }.forEachOrdered(handlers::add)
	}

	override fun getHandlers(): MutableList<String> {
		return handlers
	}
}
