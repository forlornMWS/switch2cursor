plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.github.qczone"
version = "1.0.3"

repositories {
    // 添加多个镜像源作为备选
    maven { url = uri("https://maven.aliyun.com/repository/public/") }
    maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    mavenCentral()
    gradlePluginPortal()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.1.4")  // 使用更稳定的版本
    type.set("IC") // Target IDE Platform
    pluginName.set("Switch2Cursor")
    updateSinceUntilBuild.set(true)
    sameSinceUntilBuild.set(false)

    // 尝试禁用代码检测来避免 instrumentCode 问题
    instrumentCode.set(false)

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    // 尝试修复 instrumentCode 任务
    named("instrumentCode") {
        enabled = false
    }
}
