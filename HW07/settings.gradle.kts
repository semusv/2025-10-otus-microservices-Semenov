
pluginManagement {
    // Объявление версий плагинов через переменные для централизованного управления
    val springframeworkBootVersion: String by settings
    val dependencyManagementVersion: String by settings
    val jgitVersion: String by settings
    val sonarlintVersion: String by settings
    val spotlessVersion: String by settings
    val jibVersion: String by settings
    val openapiGeneratorVersion: String by settings

    plugins {
        // Плагин для Spring Boot - упрощает создание Spring приложений
        id("org.springframework.boot") version springframeworkBootVersion

        // Плагин для управления зависимостями Spring - автоматически управляет версиями зависимостей
        id("io.spring.dependency-management") version dependencyManagementVersion

        // Плагин для автоматического управления версиями на основе Git тегов и коммитов
        id("fr.brouillard.oss.gradle.jgitver") version jgitVersion

        // Плагин для статического анализа кода SonarLint - проверка качества кода
        id("name.remal.sonarlint") version sonarlintVersion

        // Плагин для форматирования кода - автоматическое приведение кода к единому стилю
        id("com.diffplug.spotless") version spotlessVersion

        // Плагин для проверки стиля кода - обеспечивает соответствие стандартам кодирования
        id("checkstyle")

        // Плагин для создания Docker образов приложения без Dockerfile
        id("com.google.cloud.tools.jib") version jibVersion

        id("org.openapi.generator") version openapiGeneratorVersion
    }
}

rootProject.name = "hw07"

include("api-gateway")
include("auth-service")
include("user-service")
include("config-service")
include("billing-service")
include("order-service")
include("notification-service")

