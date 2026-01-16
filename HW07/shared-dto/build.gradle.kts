import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import java.util.Locale

plugins {
    idea
    `java-library`
    id("org.openapi.generator") version "7.18.0"
}

val apiContractsDir = file("../api-contracts")
val yamlFiles = apiContractsDir.listFiles { file ->
    file.isFile && (file.extension == "yaml" || file.extension == "yml")
} ?: emptyArray()

// Создаем отдельные задачи для каждого файла
yamlFiles.forEach { specFile ->
    val taskName = "generateDtoFrom${specFile.nameWithoutExtension}"

    tasks.register<GenerateTask>(taskName) {
        group = "OpenAPI"
        description = "Generates DTO models from ${specFile.name}"

        generatorName.set("spring")
        inputSpec.set(specFile.absolutePath)
        // ✅ Уникальный output директория для каждого файла
        outputDir.set(layout.buildDirectory.dir("generated-sources/openapi/${specFile.nameWithoutExtension}").get().asFile.absolutePath)
        modelPackage.set("ru.vvsem.shared.dto.${specFile.nameWithoutExtension.lowercase(Locale.getDefault())}")

        configOptions.set(
            mapOf(
                "interfaceOnly" to "true",
                "delegatePattern" to "true",
                "useJakartaEe" to "true",
                "dateLibrary" to "java8"
            )
        )

        globalProperties.set(
            mapOf(
                "apis" to "false",
                "models" to ""
            )
        )
        skipValidateSpec.set(true)
    }
}

// Создаем агрегирующую задачу, которая запускает ВСЕ генерации
tasks.register("generateDto") {
    group = "OpenAPI"
    description = "Generates DTO models from all OpenAPI specifications"

    // Зависим от всех индивидуальных задач генерации
    yamlFiles.forEach { specFile ->
        val taskName = "generateDtoFrom${specFile.nameWithoutExtension}"
        dependsOn(taskName)
    }

    doLast {
        println("✅ Generated DTOs from ${yamlFiles.size} OpenAPI specifications")
    }
}

// Обновляем sourceSets для ВСЕХ сгенерированных директорий
afterEvaluate {
    sourceSets {
        main {
            java {
                yamlFiles.forEach { specFile ->
                    srcDir(layout.buildDirectory.dir("generated-sources/openapi/${specFile.nameWithoutExtension}/src/main/java"))
                }
            }
        }
    }
}
repositories {
    mavenCentral()
}


val springdocVersion: String by project
dependencies {
//    implementation("com.fasterxml.jackson.core:jackson-databind")
//    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
//    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
//    implementation("org.springframework.boot:spring-boot-starter-validation:$springframeworkBootVersion")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")
}


tasks.compileJava {
    // ✅ Зависим от агрегирующей задачи, которая запустит все генерации
    dependsOn("generateDto")
}

// ✅ Явная зависимость spotlessJava от generateDto
tasks.named("spotlessJava") {
    dependsOn("generateDto")
}

// ✅ Отключаем Spring Boot задачи для библиотеки
tasks.named("bootJar") {
    enabled = false
}

tasks.named("bootRun") {
    enabled = false
}

// ✅ Включаем обычный jar
tasks.named("jar") {
    enabled = true
}

// ✅ Добавляем задачу для очистки всех сгенерированных файлов
tasks.register("cleanGenerated") {
    group = "cleanup"
    description = "Cleans all generated OpenAPI files"

    doLast {
        yamlFiles.forEach { specFile ->
            val generatedDir = layout.buildDirectory.dir("generated-sources/openapi/${specFile.nameWithoutExtension}").get().asFile
            if (generatedDir.exists()) {
                delete(generatedDir)
                println("Cleaned generated directory: ${generatedDir.name}")
            }
        }
    }
}

// ✅ Связываем стандартную clean задачу с нашей cleanGenerated
tasks.named("clean") {
    dependsOn("cleanGenerated")
}