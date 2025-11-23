plugins {
	id("com.google.cloud.tools.jib")
}

dependencies {
//	// Логирование в JSON формате
//	implementation("net.logstash.logback:logstash-logback-encoder")

	// Мониторинг и метрики
	implementation("org.springframework.boot:spring-boot-starter-actuator")
//	implementation("io.micrometer:micrometer-registry-prometheus")

	// Web фреймворк
	implementation("org.springframework.boot:spring-boot-starter-web")

	// Spring Cloud
//	implementation("org.springframework.cloud:spring-cloud-starter-config")					// Config Server
//	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")  // Service Discovery
//	implementation("org.springframework.cloud:spring-cloud-starter-openfeign")				// REST клиент

//	// Метрики
//	implementation("io.micrometer:micrometer-tracing-bridge-otel")   // bridges the Micrometer Observation API to OpenTelemetry.
//	implementation("io.opentelemetry:opentelemetry-exporter-zipkin") // reports traces to Zipkin.
//	implementation("io.github.openfeign:feign-micrometer")

//	implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
}

jib {
	container {
		creationTime.set("USE_CURRENT_TIMESTAMP")
		ports = listOf("8000")
	}
	from {
		image = "bellsoft/liberica-openjdk-alpine-musl:21.0.1"
	}

	to {
		image = "vvsem/simple-service"
		tags = setOf("latest", project.version.toString())
//		image = "localrun/simple-service"
//		tags = setOf(project.version.toString())
	}
}


