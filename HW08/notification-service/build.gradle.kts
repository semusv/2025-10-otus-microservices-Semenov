val mapStructVersion: String by project
val lombokMapstructBindingVersion: String by project
val springwolfVersion: String by project
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")

    //swagger
    implementation ("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    implementation("io.github.springwolf:springwolf-kafka:${springwolfVersion}")
    implementation("io.github.springwolf:springwolf-ui:${springwolfVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    compileOnly("org.mapstruct:mapstruct:$mapStructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapStructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")

    // DTO
    implementation(project(":shared-dto"))
}

