plugins {
    id("com.murex.mxorbit.java-application-conventions")
    id("com.diffplug.spotless")
}

group = "com.murex.mxorbit"

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.defaultComponentModel=spring",
        "-Amapstruct.unmappedTargetPolicy=ERROR"
    ))
}

spotless {
    java {
        eclipse()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    implementation("org.mapstruct:mapstruct")
    implementation("io.temporal:temporal-spring-boot-starter")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor")

    runtimeOnly("org.postgresql:postgresql")
}
