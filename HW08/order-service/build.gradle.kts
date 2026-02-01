val mapStructVersion: String by project
val lombokMapstructBindingVersion: String by project
val springwolfVersion: String by project
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    //  Kafka
    implementation("org.springframework.kafka:spring-kafka")

    //  Metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation ("io.micrometer:micrometer-core")

    //  OpenAPI
    implementation ("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    implementation("io.github.springwolf:springwolf-kafka:${springwolfVersion}")
    implementation("io.github.springwolf:springwolf-ui:${springwolfVersion}")

    //  Database
    implementation("org.liquibase:liquibase-core")
    implementation("org.postgresql:postgresql")

    //  Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    //  MapStruct
    compileOnly("org.mapstruct:mapstruct:$mapStructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapStructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")

    // DTO
    implementation(project(":shared-dto"))
}

