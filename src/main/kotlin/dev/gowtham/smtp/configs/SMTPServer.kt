package dev.gowtham.smtp.configs

import com.google.common.collect.ImmutableList
import com.google.inject.TypeLiteral
import dev.gowtham.smtp.utils.SMTPUtil
import org.apache.commons.configuration2.BaseHierarchicalConfiguration
import org.apache.commons.configuration2.XMLConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.io.FileHandler
import org.apache.james.UserEntityValidator
import org.apache.james.dnsservice.api.DNSService
import org.apache.james.dnsservice.dnsjava.DNSJavaService
import org.apache.james.domainlist.api.DomainList
import org.apache.james.domainlist.memory.MemoryDomainList
import org.apache.james.filesystem.api.FileSystem
import org.apache.james.mailrepository.api.MailRepositoryStore
import org.apache.james.mailrepository.api.Protocol
import org.apache.james.mailrepository.memory.*
import org.apache.james.metrics.api.MetricFactory
import org.apache.james.metrics.logger.DefaultMetricFactory
import org.apache.james.queue.api.MailQueueFactory
import org.apache.james.queue.api.RawMailQueueItemDecoratorFactory
import org.apache.james.queue.memory.MemoryMailQueueFactory
import org.apache.james.rrt.api.CanSendFrom
import org.apache.james.rrt.api.RecipientRewriteTable
import org.apache.james.rrt.api.RecipientRewriteTableConfiguration
import org.apache.james.rrt.lib.AliasReverseResolverImpl
import org.apache.james.rrt.lib.CanSendFromImpl
import org.apache.james.rrt.memory.MemoryRecipientRewriteTable
import org.apache.james.server.core.filesystem.FileSystemImpl
import org.apache.james.smtpserver.netty.SMTPServerFactory
import org.apache.james.user.api.UsersRepository
import org.apache.james.user.memory.MemoryUsersRepository
import org.jboss.netty.util.HashedWheelTimer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.io.File

@Component
class SMTPServer {
	companion object {
		private val log = LoggerFactory.getLogger(this::class.java.name)
	}

	@Autowired
	private lateinit var smtpUtil: SMTPUtil

	private var smtpServerFactory: SMTPServerFactory? = null

	private lateinit var dnsService: DNSService

	private lateinit var fileSystem: FileSystem

	private val metricFactory: MetricFactory = DefaultMetricFactory()

	private val hashedWheelTimer: HashedWheelTimer = HashedWheelTimer()

	@Value("\${smtp.server-enabled}")
	private var serverEnabled: Boolean = true

	private lateinit var domainList: MemoryDomainList
	private lateinit var usersRepository: MemoryUsersRepository
	private lateinit var queueFactory: MemoryMailQueueFactory
	private lateinit var rewriteTable: MemoryRecipientRewriteTable
	private lateinit var canSendFrom: CanSendFrom
	private lateinit var mailRepositoryStore: MemoryMailRepositoryStore

	fun start() {
		if (!serverEnabled) {
			log.info("SMTP server is not enabled. won't run")
			return
		}
		setup()
		val protocolHandlerLoader = SMTPProtocolHandlerLoader.builder()
			.put { binder -> binder.bind(DomainList::class.java).toInstance(domainList) }
			.put { binder -> binder.bind(object : TypeLiteral<MailQueueFactory<*>?>() {}).toInstance(queueFactory) }
			.put { binder -> binder.bind(RecipientRewriteTable::class.java).toInstance(rewriteTable) }
			.put { binder -> binder.bind(CanSendFrom::class.java).toInstance(canSendFrom) }
			.put { binder -> binder.bind(FileSystem::class.java).toInstance(fileSystem) }
			.put { binder -> binder.bind(MailRepositoryStore::class.java).toInstance(mailRepositoryStore) }
			.put { binder -> binder.bind(DNSService::class.java).toInstance(dnsService) }
			.put { binder -> binder.bind(UsersRepository::class.java).toInstance(usersRepository) }
			.put { binder -> binder.bind(MetricFactory::class.java).toInstance(metricFactory) }
			.put { binder -> binder.bind(UserEntityValidator::class.java).toInstance(UserEntityValidator.NOOP) }
			// custom injections start here
			.put { binder -> binder.bind(SMTPUtil::class.java).toInstance(smtpUtil) }
			.build()

		smtpServerFactory =
			SMTPServerFactory(dnsService, protocolHandlerLoader, fileSystem, metricFactory, hashedWheelTimer)

		// read the properties
		val params = Parameters()
		val configuration = FileBasedConfigurationBuilder(XMLConfiguration::class.java)
			.configure(params.xml()).configuration

		// see under config directory
		val externalFile = File("config/smtp/smtpserver.xml")
		if (externalFile.exists()) {
			log.info("SMTP config will be read from config/smtp dir")
			val fileHandler = FileHandler(configuration)
			fileHandler.load(externalFile.inputStream())
		} else {
			log.info("SMTP config will be read from classpath")
			val xmlFileResource = this.javaClass.classLoader.getResourceAsStream("smtp/smtpserver.xml")
			val fileHandler = FileHandler(configuration)
			fileHandler.load(xmlFileResource)
		}

		smtpServerFactory!!.configure(configuration)
		smtpServerFactory!!.init()
	}

	private fun setup() {
		dnsService = getDnsService()
		domainList = MemoryDomainList(dnsService)
		usersRepository = MemoryUsersRepository.withVirtualHosting(domainList)

		queueFactory = MemoryMailQueueFactory(RawMailQueueItemDecoratorFactory())

		rewriteTable = MemoryRecipientRewriteTable()
		rewriteTable.configuration = RecipientRewriteTableConfiguration.DISABLED

		val aliasReverseResolver = AliasReverseResolverImpl(rewriteTable)
		canSendFrom = CanSendFromImpl(rewriteTable, aliasReverseResolver)

		val configuration = org.apache.james.server.core.configuration.Configuration.builder()
			.workingDirectory("../")
			.configurationFromClasspath()
			.build()
		fileSystem = FileSystemImpl(configuration.directories())

		createMailRepositoryStore()
	}

	private fun getDnsService(): DNSService {
		// read the properties
		val params = Parameters()
		val configuration = FileBasedConfigurationBuilder(XMLConfiguration::class.java)
			.configure(params.xml()).configuration

		// see under config directory
		val externalFile = File("config/smtp/dnsservice.xml")
		if (externalFile.exists()) {
			log.info("DNS Service config will be read from config/smtp dir")
			val fileHandler = FileHandler(configuration)
			fileHandler.load(externalFile.inputStream())
		} else {
			log.info("DNS Service config will be read from classpath")
			val xmlFileResource = this.javaClass.classLoader.getResourceAsStream("smtp/dnsservice.xml")
			val fileHandler = FileHandler(configuration)
			fileHandler.load(xmlFileResource)
		}

		val dnsService = DNSJavaService(metricFactory)
		dnsService.configure(configuration)
		return dnsService
	}

	private fun createMailRepositoryStore() {
		val urlStore = MemoryMailRepositoryUrlStore()
		val configuration = MailRepositoryStoreConfiguration.forItems(
			MailRepositoryStoreConfiguration.Item(
				ImmutableList.of(Protocol("memory")),
				MemoryMailRepository::class.java.name,
				BaseHierarchicalConfiguration()
			)
		)
		mailRepositoryStore = MemoryMailRepositoryStore(urlStore, SimpleMailRepositoryLoader(), configuration)
		mailRepositoryStore.init()
	}

	fun stop() {
		if (smtpServerFactory?.servers == null) {
			return
		}
		smtpServerFactory?.destroy()
		hashedWheelTimer.stop()
	}
}

@Configuration
class SMTPServerConfiguration {

	@Bean(initMethod = "start", destroyMethod = "stop")
    fun smtpServer() = SMTPServer()
}
