plugins {
    id("com.murex.mxorbit.java-application-conventions")
    id("com.diffplug.spotless")
}

group = "com.murex.mxorbit"

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "-Amapstruct.defaultComponentModel=spring",
            "-Amapstruct.unmappedTargetPolicy=ERROR"
        )
    )
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

    // Task 6: OAuth 2.0 resource-server foundation, disabled by default until the
    // real MXORBIT API audience and delegated scope are available (see Task 7).
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.mapstruct:mapstruct-processor")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.temporal:temporal-testing")
    // Minimum test infrastructure needed for Task 6 context/slice tests; no
    // security tests or context tests previously existed in this module.
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
