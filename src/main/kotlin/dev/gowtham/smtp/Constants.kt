package dev.gowtham.smtp

import org.apache.james.protocols.api.ProtocolSession
import java.util.*

object Constants {
	private const val SESSION_ID = "session_id"
	private const val SESSION_START_TIME = "session_start"

	// session attachments
	val SA_SESSION_ID: ProtocolSession.AttachmentKey<UUID> =
		ProtocolSession.AttachmentKey.of(SESSION_ID, UUID::class.java)
	val SA_START_TIME: ProtocolSession.AttachmentKey<Long> =
		ProtocolSession.AttachmentKey.of(SESSION_START_TIME, Long::class.java)
}
