package dev.gowtham.smtp.configs

import com.google.common.collect.ImmutableList
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import org.apache.commons.configuration2.Configuration
import org.apache.james.protocols.api.handler.ProtocolHandler
import org.apache.james.protocols.lib.handler.ProtocolHandlerLoader
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

class SMTPProtocolHandlerLoader private constructor(private val injector: Injector) : ProtocolHandlerLoader {

	private val loaderRegistry: MutableList<Any> = ArrayList()

	override fun load(name: String, config: Configuration): ProtocolHandler {
		return try {
			val aClass = Thread.currentThread().contextClassLoader.loadClass(name)
			val obj = injector.getInstance(aClass) as ProtocolHandler
			postConstruct(obj)
			init(obj, config)
			synchronized(this) { loaderRegistry.add(obj) }
			obj
		} catch (e: Exception) {
			throw ProtocolHandlerLoader.LoadingException("Unable to load protocolhandler", e)
		}
	}

	/**
	 * Dispose all loaded instances by calling the method of the instances which
	 * is annotated with @PreDestroy
	 */
	@Synchronized
	fun dispose() {
		for (aLoaderRegistry in loaderRegistry) {
			try {
				preDestroy(aLoaderRegistry)
			} catch (e: IllegalAccessException) {
				e.printStackTrace()
			} catch (e: InvocationTargetException) {
				e.printStackTrace()
			}
		}
		loaderRegistry.clear()
	}

	@Throws(IllegalAccessException::class, InvocationTargetException::class)
	private fun postConstruct(resource: Any) {
		val methods = resource.javaClass.methods
		for (method in methods) {
			val postConstructAnnotation = method.getAnnotation(
					PostConstruct::class.java
			)
			if (postConstructAnnotation != null) {
				val args = arrayOf<Any>()
				method.invoke(resource, *args)
			}
		}
	}

	@Throws(IllegalAccessException::class, InvocationTargetException::class)
	private fun preDestroy(resource: Any) {
		val methods = resource.javaClass.methods
		for (method in methods) {
			val preDestroyAnnotation = method.getAnnotation(
					PreDestroy::class.java
			)
			if (preDestroyAnnotation != null) {
				val args = arrayOf<Any>()
				method.invoke(resource, *args)
			}
		}
	}

	@Throws(IllegalAccessException::class, InvocationTargetException::class)
	private fun init(resource: Any, config: Configuration) {
		val methods = resource.javaClass.methods
		for (method in methods) {
			if (isInit(method)) {
				val args = arrayOf<Any>(config)
				method.invoke(resource, *args)
			}
		}
	}

	private fun isInit(method: Method): Boolean {
		return method.name == "init" && method.parameterTypes.size == 1 && method.parameterTypes[0] == Configuration::class.java
	}

	companion object {
		class Builder {
			private val modules: ImmutableList.Builder<Module> = ImmutableList.builder()

			fun put(module: Module): Builder {
				modules.add(module)
				return this
			}

			fun build(): SMTPProtocolHandlerLoader {
				return SMTPProtocolHandlerLoader(Guice.createInjector(modules.build()))
			}
		}

		fun builder(): Builder {
			return Builder()
		}
	}
}
