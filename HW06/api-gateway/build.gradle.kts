dependencies {
    implementation ("org.springframework.boot:spring-boot-starter-webflux")

    implementation ("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")

    implementation ("org.springframework.cloud:spring-cloud-starter-config")

    implementation ("org.springframework.boot:spring-boot-starter-actuator")
    //micrometer
    implementation ("io.micrometer:micrometer-registry-prometheus")
    implementation ("io.micrometer:micrometer-core")
}


