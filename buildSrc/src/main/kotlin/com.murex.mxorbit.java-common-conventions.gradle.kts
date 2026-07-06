plugins {
    `java-library`
    java
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/release") }
}

extra["springBootVersion"] = "4.0.7"
extra["springCloudVersion"] = "2025.1.1"

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${property("springBootVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("io.temporal:temporal-bom:1.29.0")
    }
    dependencies {
        dependency("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
        dependency("org.mapstruct:mapstruct:1.6.3")
        dependency("org.mapstruct:mapstruct-processor:1.6.3")
    }
}
