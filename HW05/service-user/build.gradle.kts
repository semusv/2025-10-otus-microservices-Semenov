//plugins {
//	id("com.google.cloud.tools.jib")
//}

dependencies {

	// Мониторинг и метрики
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")

	// Web фреймворк
	implementation("org.springframework.boot:spring-boot-starter-web")

	//БД
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")


	// Spring Cloud
//	implementation("org.springframework.cloud:spring-cloud-starter-config")					// Config Server
//	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")  // Service Discovery
//	implementation("org.springframework.cloud:spring-cloud-starter-openfeign")				// REST клиент


//	// Логирование в JSON формате
//	implementation("net.logstash.logback:logstash-logback-encoder")


//	// Метрики
//	implementation("io.micrometer:micrometer-tracing-bridge-otel")   // bridges the Micrometer Observation API to OpenTelemetry.
//	implementation("io.opentelemetry:opentelemetry-exporter-zipkin") // reports traces to Zipkin.
//	implementation("io.github.openfeign:feign-micrometer")

//	implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

	// Lombok
	implementation("org.projectlombok:lombok")

	//swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")

	//hateoas
	implementation("org.springframework.boot:spring-boot-starter-hateoas")

    //Liquibase
    implementation("org.liquibase:liquibase-core")

	//mapstruct
	compileOnly("org.mapstruct:mapstruct:1.6.0")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.0")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	//БД
	runtimeOnly("org.postgresql:postgresql")

}

//jib {
//	container {
//		creationTime.set("USE_CURRENT_TIMESTAMP")
//		ports = listOf("8000")
//	}
//	from {
//		image = "bellsoft/liberica-openjdk-alpine-musl:21.0.1"
//	}
//
//	to {
//		image = "${property("dockerRegistry")}/${project.name}"
//		tags = setOf("latest", property("projectVersion").toString())
//	}
//}
