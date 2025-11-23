plugins {
	id("com.google.cloud.tools.jib")
}

dependencies {
	// Мониторинг и метрики
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// Web фреймворк
	implementation("org.springframework.boot:spring-boot-starter-web")
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
	}
}


