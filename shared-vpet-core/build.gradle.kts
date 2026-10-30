plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
}

tasks.withType<Test> {
    systemProperty("generateDocs", System.getProperty("generateDocs") ?: "false")
}
