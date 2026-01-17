import com.diffplug.gradle.spotless.SpotlessExtension
import com.google.cloud.tools.jib.gradle.JibExtension
import fr.brouillard.oss.gradle.plugins.JGitverPluginExtension
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

import org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES

plugins {
    idea
    application
    id("fr.brouillard.oss.gradle.jgitver")      // Автоматическое версионирование через Git
    id("io.spring.dependency-management")       // Управление зависимостями (аналог Maven BOM)
    id("org.springframework.boot") apply false  // Spring Boot, но не применяем к корню
    id("name.remal.sonarlint") apply false      // Статический анализ кода
    id("com.diffplug.spotless") apply false     // Форматирование кода
    id("checkstyle")                            // Проверка стиля кода
    id("com.google.cloud.tools.jib") apply false  // Деплой в Docker
    id("org.openapi.generator") apply false

}


// Общие настройки для всех проектов
allprojects {
    group = "ru.vvsem"  // Аналог <groupId> в Maven

    repositories {
        mavenLocal()    // Локальный Maven репозиторий (~/.m2)
        mavenCentral()  // Maven Central
    }


    apply(plugin = "io.spring.dependency-management")


    // Включить кэширование тестов
    tasks.withType<Test> {
        outputs.cacheIf { true }
    }

    // Кэширование компиляции
    tasks.withType<JavaCompile> {
        inputs.property("moduleName", project.name)
        outputs.cacheIf { true }
    }

    // Настройка инкрементальной компиляции для аннотаций
    tasks.withType<JavaCompile> {
        options.isIncremental = true
    }
}

// Настройки для всех подпроектов (микросервисов)
subprojects {
    // Управление зависимостями (аналог Maven Dependency Management)
    val springCloudVersion: String by project
    val logbackEncoder: String by project
    val lombokVersion: String by project
    val springdocVersion: String by project
    val hateoasVersion: String by project

    dependencyManagement {
        dependencies {
            imports {
                // Импортируем BOM'ы (аналог <dependencyManagement> в Maven)
                mavenBom(BOM_COORDINATES)  // Spring Boot BOM
                mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
            }
            dependency("net.logstash.logback:logstash-logback-encoder:$logbackEncoder")
            dependency("org.projectlombok:lombok:$lombokVersion")
            dependency("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
            dependency("org.springframework.boot:spring-boot-starter-hateoas:$hateoasVersion")
        }
    }


    // Применяем плагины ко всем подпроектам
    apply(plugin = "org.springframework.boot")
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")
//    apply(plugin = "name.remal.sonarlint")
    apply(plugin = "fr.brouillard.oss.gradle.jgitver")
    apply(plugin = "checkstyle")
    apply(plugin = "com.google.cloud.tools.jib")
    apply(plugin = "idea")


    configure<SpotlessExtension> {
        java {
            palantirJavaFormat("2.38.0")  // Автоматическое форматирование кода
            targetExclude("${layout.buildDirectory.get()}/generated/**")
        }
    }

    val contractFile = file("../api-contracts/${project.name}-api.yaml")
    //пока не будем использовать
    if (contractFile.exists() && 1 == 2) {
        apply(plugin = "org.openapi.generator")

        // ✅ УДАЛИ кастомную задачу, используй openApiGenerate
        tasks.named("openApiGenerate", GenerateTask::class.java) {
            generatorName.set("spring")
            inputSpec.set(contractFile.absolutePath)
            outputDir.set("${layout.buildDirectory.get()}/generated/sources/openapi")
            apiPackage.set("ru.vvsem.${project.name}.api")
            modelPackage.set("ru.vvsem.${project.name}.dto")
            invokerPackage.set("ru.vvsem.${project.name}.invoker")
            configOptions.set(
                mapOf(
                    "interfaceOnly" to "true",
                    "delegatePattern" to "true",
                    "useJakartaEe" to "true",
                    "dateLibrary" to "java8"
                )
            )
            validateSpec.set(true)
        }

        the<JavaPluginExtension>().sourceSets {
            getByName("main") {
                java.srcDir("${layout.buildDirectory.get()}/generated/sources/openapi/src/main/java")
            }
        }

        tasks.named("compileJava") {
            dependsOn("openApiGenerate") // ✅ имя стандартное
        }

        tasks.named("spotlessJava") {
            dependsOn("openApiGenerate")
        }
    }

    afterEvaluate {
        dependencies {
            add("compileOnly", "org.projectlombok:lombok")
            add("annotationProcessor", "org.projectlombok:lombok")

            // ✅ ДОБАВИТЬ: Jackson для работы с DTO
            add("implementation", "com.fasterxml.jackson.core:jackson-databind")
            add("implementation", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

            // ✅ ДОБАВИТЬ: Bean Validation
            add("implementation", "jakarta.validation:jakarta.validation-api")
            add("implementation", "org.hibernate.validator:hibernate-validator")


            add("testCompileOnly", "org.projectlombok:lombok")
            add("testAnnotationProcessor", "org.projectlombok:lombok")
        }
    }


    configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    // Настройки компиляции
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-processing"))
        dependsOn("spotlessApply")
    }

    // Настройка Checkstyle
    configure<CheckstyleExtension> {
        val checkstylePluginVersion = rootProject.property("checkstyle.version") as String
        val checkstyleConfigUrl = rootProject.property("checkstyle.config.url") as String
        toolVersion = checkstylePluginVersion  // Используем версию из gradle.properties
        config = resources.text.fromUri(checkstyleConfigUrl)
        maxErrors = 0
        maxWarnings = 0
        isIgnoreFailures = false
    }

    // Исключаем проверку сгенерированных классов
    tasks.withType<Checkstyle>().configureEach {
        classpath = project.files()
        exclude("**/generated/**")
        exclude("**/build/**")

        // Явное указание source directories
        source = fileTree("src/main/java") {
            include("**/*.java")
            exclude("**/generated/**")
        }
    }


    // Настройки тестов
    tasks.withType<Test> {
        useJUnitPlatform()  // Используем JUnit 5 (аналог surefire-plugin в Maven)
        testLogging.showExceptions = true
        reports {
            junitXml.required.set(true) // XML отчеты для CI/CD
            html.required.set(true)     // HTML отчеты
        }
    }

    // Автоматическое версионирование
    //автоматически генерирует версии на основе Git тегов и коммитов.
//    extensions.configure<JGitverPluginExtension> {
//        strategy("PATTERN")
//        nonQualifierBranches("main,master")
//        tagVersionPattern("\${v}\${<meta.DIRTY_TEXT}")
//        versionPattern(
//            "\${v}\${<meta.COMMIT_DISTANCE}\${<meta.GIT_SHA1_8}" +
//                    "\${<meta.QUALIFIED_BRANCH_NAME}\${<meta.DIRTY_TEXT}-SNAPSHOT"
//        )
//    }
    extensions.configure<JGitverPluginExtension> {
        strategy("PATTERN")
        nonQualifierBranches("main,master")
        // Более чистый паттерн для версий
        versionPattern(
            "\${v}\${<meta.COMMIT_DISTANCE}-SNAPSHOT"
        )
        // Не добавляем dirty флаг в версию
        useDirty(false)
    }

    extensions.configure<JibExtension> {
        from {
            image = "bellsoft/liberica-openjdk-alpine-musl:21.0.1"
        }
        to {
            image = "vvsem/${rootProject.name}-${project.name}"
            tags = setOf("latest", project.version.toString())
        }

        container {
            creationTime.set("USE_CURRENT_TIMESTAMP")

            // Динамические порты для разных сервисов
            val port = when (project.name) {
                "config-service" -> "8888"
                "user-service" -> "8082"
                "auth-service" -> "8081"
                "billing-service" -> "8083"
                "order-service" -> "8084"
                "notification-service" -> "8085"
                "api-gateway" -> "8000"
                else -> "8080"
            }
            ports = listOf(port)

            // Общие JVM флаги для всех сервисов
            jvmFlags = listOf(
                "-Xmx512m",
                "-Xms256m"
            )
            environment = mapOf(
                "JAVA_TOOL_OPTIONS" to "-Dfile.encoding=UTF-8"
            )
        }
    }
}


tasks {
    register("printManagedVersions") {
        doLast {
            project.extensions.getByType<DependencyManagementExtension>()
                .managedVersions
                .toSortedMap()
                .map { "${it.key}:${it.value}" }
                .forEach(::println)
        }
    }

    register<Exec>("composeUp") {
        group = "docker"
//        description = "Build all images and start full docker-compose"
//        dependsOn(getSubprojectJibTasks())

        workingDir = file("docker/compose")
        commandLine("docker-compose", "up", "-d")

        doLast {
            println("=== Microservices Stack Started ===")
            println("Gateway: http://localhost")
            println("Licensing: http://localhost:8085")
            println("Config: http://localhost:8071")
        }
    }

    register<Exec>("composeUpDev") {
        group = "docker"
        description = "Start with development overrides"
        dependsOn(getSubprojectJibTasks())

        workingDir = file("docker/compose")
        commandLine("docker", "compose", "-f", "docker-compose.yml", "-f", "docker-compose.dev.yml", "up", "-d")
    }

    register<Exec>("composeDown") {
        group = "docker"
        description = "Stop all services"

        workingDir = file("docker/compose")
        commandLine("docker-compose", "down")
    }

    register<Exec>("composeLogs") {
        group = "docker"
        description = "Show logs for all services"

        workingDir = file("docker/compose")
        commandLine("docker-compose", "logs", "-f")
    }

    register<Exec>("composePs") {
        group = "docker"
        description = "Show status of all services"

        workingDir = file("docker/compose")
        commandLine("docker-compose", "ps")
    }

    register("composeRestart") {
        group = "docker"
        description = "Restart all services"
        dependsOn("composeDown", "composeUp")
    }

//    // Задачи для отдельных сервисов
//    register<Exec>("composeUpLicensing") {
//        group = "docker"
//        description = "Start only licensing service with dependencies"
//        dependsOn(":licensing-service:jibDockerBuild")
//
//        workingDir = file("docker/compose")
//        commandLine(
//            "docker-compose", "up", "-d",
//            "licensing-service",
//            "postgres",
//            "config-server"
//        )
//    }

    register<Exec>("composeUpConfig") {
        group = "docker"
        description = "Start only Config server with dependencies"
        dependsOn(":config-server:jibDockerBuild")

        workingDir = file("docker/compose")
        commandLine(
            "docker-compose", "up", "-d",
            "config-service",
            "postgres"
        )
    }

    // Вспомогательная задача для сборки всех образов
    register("buildAllImages") {
        group = "docker"
        description = "Build Docker images for all services"
        dependsOn(getSubprojectJibTasks())
    }
}

// Функция для получения всех jibDockerBuild задач из субпроектов
fun getSubprojectJibTasks(): List<Task> {

    return subprojects
        .filter { it.name != "shared-dto" }
        .map { project -> project.tasks.getByName("jibDockerBuild")
    }
}
