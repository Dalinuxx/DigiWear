plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

tasks.register("syncReleaseBin") {
    group = "distribution"
    description = "Copies canonical branded release APKs into release_bin and prunes unbranded/redundant APKs."
    dependsOn(":wear-os-app:assembleRelease", ":phone-companion-app:assembleRelease")
    doLast {
        val releaseBin = file("release_bin").apply { mkdirs() }

        // Remove any obsolete unbranded generic Gradle APKs if present
        fileTree(releaseBin) {
            include("wear-os-app-release.apk", "phone-companion-app-release.apk")
        }.forEach { it.delete() }

        // Sync Wear OS APK -> DigiWear-WearOS-release.apk & digiwear-aosp-standard-watch-release.apk
        val wearRelease = file("wear-os-app/build/outputs/apk/release/DigiWear-WearOS-release.apk").let {
            if (it.exists()) it else file("wear-os-app/build/outputs/apk/release/wear-os-app-release.apk")
        }
        if (wearRelease.exists()) {
            wearRelease.copyTo(File(releaseBin, "DigiWear-WearOS-release.apk"), overwrite = true)
            wearRelease.copyTo(File(releaseBin, "digiwear-aosp-standard-watch-release.apk"), overwrite = true)
        }

        // Sync Companion APK -> DigiWear-Companion-release.apk
        val phoneRelease = file("phone-companion-app/build/outputs/apk/release/DigiWear-Companion-release.apk").let {
            if (it.exists()) it else file("phone-companion-app/build/outputs/apk/release/phone-companion-app-release.apk")
        }
        if (phoneRelease.exists()) {
            phoneRelease.copyTo(File(releaseBin, "DigiWear-Companion-release.apk"), overwrite = true)
        }
    }
}
