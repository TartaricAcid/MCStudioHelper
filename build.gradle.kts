import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("idea")
    id("org.jetbrains.kotlin.jvm") version "2.2.21"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.github.tartaricacid.mcshelper"
version = "1.1.0"

fun environment(key: String) = providers.environmentVariable(key)

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        pycharmCommunity("2025.2.3")
        bundledPlugin("PythonCore")

        plugin("com.redhat.devtools.lsp4ij:0.19.0")

        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }
    }

    signing {
        certificateChain = environment("JB_CERTIFICATE_CHAIN")
        privateKey = environment("JB_PRIVATE_KEY")
        password = environment("JB_PRIVATE_KEY_PASSWORD")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
