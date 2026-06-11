plugins {
    id("org.springframework.boot") version "3.4.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
    kotlin("plugin.jpa") version "1.9.24" apply false
    id("org.sonarqube") version "5.1.0.4882"
}

sonarqube {
    properties {
        property("sonar.projectKey", "ThomasBrueck_finalproject-ingesoftv-dev")
        property("sonar.organization", "ThomasBrueck")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}

allprojects {
    group = "com.circleguard"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "jacoco")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        "implementation"(platform("org.springframework.boot:spring-boot-dependencies:3.4.5"))
        "testImplementation"(platform("org.springframework.boot:spring-boot-dependencies:3.4.5"))
        "compileOnly"("org.projectlombok:lombok")
        "annotationProcessor"("org.projectlombok:lombok")
        "testCompileOnly"("org.projectlombok:lombok")
        "testAnnotationProcessor"("org.projectlombok:lombok")
        "implementation"("org.jetbrains.kotlin:kotlin-reflect")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("com.h2database:h2")
        "testImplementation"("org.testcontainers:junit-jupiter:1.20.1")
        "testImplementation"("org.testcontainers:postgresql:1.20.1")
        "testImplementation"("org.testcontainers:kafka:1.20.1")
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform {
            excludeTags("integration")
        }
        finalizedBy(tasks.named("jacocoTestReport"))
        exclude("com.circleguard.e2e.**")
    }

    tasks.register<Test>("integrationTest") {
        useJUnitPlatform {
            includeTags("integration")
        }
        // Don't inherit the e2e exclude from parent
        excludes.clear()
    }

    tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/*Application*",
                        "**/model/**",
                        "**/dto/**",
                        "**/config/**",
                        "**/exception/**",
                        "**/event/**"
                    )
                }
            })
        )
    }

    tasks.named<org.gradle.testing.jacoco.tasks.JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        classDirectories.setFrom(
            files(classDirectories.files.map {
                fileTree(it) {
                    exclude(
                        "**/*Application*",
                        "**/model/**",
                        "**/dto/**",
                        "**/config/**",
                        "**/exception/**",
                        "**/event/**"
                    )
                }
            })
        )
        violationRules {
            rule {
                limit {
                    minimum = "0.70".toBigDecimal()
                }
            }
        }
    }
}
