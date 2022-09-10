package dev.gowtham.smtp.utils

import dev.gowtham.smtp.Constants.SA_SESSION_ID
import dev.gowtham.smtp.Constants.SA_START_TIME
import org.apache.james.protocols.api.ProtocolSession
import org.springframework.stereotype.Component
import java.util.*

@Component
class SMTPUtil {
	fun setTransactionUUID(session: ProtocolSession) {
		if (!session.getAttachment(SA_SESSION_ID, ProtocolSession.State.Transaction).isPresent) {
			session.setAttachment(SA_SESSION_ID, UUID.randomUUID(), ProtocolSession.State.Transaction)
		}
	}

	fun setSessionUUID(session: ProtocolSession) {
		if (!session.getAttachment(SA_SESSION_ID, ProtocolSession.State.Connection).isPresent) {
			session.setAttachment(SA_SESSION_ID, UUID.randomUUID(), ProtocolSession.State.Connection)
		}
	}

	fun initSessionTimer(session: ProtocolSession) {
		if (!session.getAttachment(SA_START_TIME, ProtocolSession.State.Connection).isPresent) {
			session.setAttachment(SA_START_TIME, System.currentTimeMillis(), ProtocolSession.State.Connection)
		}
	}

	fun getTransactionUUID(session: ProtocolSession): UUID? {
		return session.getAttachment(SA_SESSION_ID, ProtocolSession.State.Transaction).orElse(null)
	}

	fun getSessionUUID(session: ProtocolSession): UUID? {
		return session.getAttachment(SA_SESSION_ID, ProtocolSession.State.Connection).orElse(null)
	}

	fun getSessionLog(session: ProtocolSession): String {
		return "Session ID: ${session.sessionID} Session UUID: ${getSessionUUID(session)}, Transaction UUID: ${
			getTransactionUUID(session)
		}, IP: ${session.remoteAddress}"
	}
}
