plugins {
    id("com.murex.mxorbit.java-common-conventions")
    id("org.springframework.boot")
    application
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
