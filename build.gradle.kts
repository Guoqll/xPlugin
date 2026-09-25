plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.frame.plugin"
// 允许通过命令行参数传入版本号，如果没有传入，则默认使用 "1.0.1"
// 使用方式: ./gradlew buildPlugin -PpluginVersion=1.0.2
version = project.findProperty("pluginVersion") ?: "1.0.2"

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
    // 尝试使用大版本号，避免具体的小版本下载链接失效引发 NPE
    version.set("2024.1")
    type.set("IC") // Target IDE Platform

    // Dart PSI is used by the optional GetX route generator. This version is
    // compatible with the 2024.1 (241) IntelliJ platform used by this project.
    plugins.set(listOf("Dart:241.15845"))
}

dependencies {
    testImplementation("junit:junit:4.13.2")
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
        sinceBuild.set("241")
        untilBuild.set("253.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    buildPlugin {
        doLast {
            // 获取当前用户的 Downloads 目录 (Mac: ~/Downloads)
            val downloadsDir = file(System.getProperty("user.home") + "/Downloads")
            
            // 将生成的产物（.jar 或 .zip）复制到 Downloads 目录
            copy {
                from(archiveFile)
                into(downloadsDir)
            }
            
            println("======> 打包成功！插件已复制到: ${downloadsDir.absolutePath}/${archiveFile.get().asFile.name}")
        }
    }
}
