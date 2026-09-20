plugins {
    kotlin("jvm")
}

group = "ua.com.jit.modules"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("ua.com.jit:module-runtime-api:0.1.0-SNAPSHOT")
    testImplementation(kotlin("test-junit"))
}
