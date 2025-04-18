plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.intellij") version "1.13.1"
}

group = "com.frame.plugin"
version = "1.0.0"

repositories {
    // 优先使用阿里云镜像（推荐）
    maven { url = uri("https://maven.aliyun.com/repository/public/") }
    // 腾讯云镜像（备选）
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    // 华为云镜像（次备选）
    maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
    // 官方仓库（兜底）
    mavenCentral()
    // IntelliJ 插件专用仓库（如果依赖未同步到镜像）
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.2.4")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("222")
        untilBuild.set("243.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
