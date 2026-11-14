import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val debugApiBaseUrl = providers.gradleProperty("NIHONIX_DEBUG_API_BASE_URL")
    .orElse("http://10.0.2.2:3010/api/")
val releaseApiBaseUrl = providers.gradleProperty("NIHONIX_RELEASE_API_BASE_URL")
    .orElse(providers.environmentVariable("NIHONIX_RELEASE_API_BASE_URL"))
    .orElse("")

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r") + "\""

val validateReleaseEnvironment = tasks.register("validateReleaseEnvironment") {
    group = "verification"
    description = "Require an explicit HTTPS API URL before packaging a release."
    inputs.property("releaseApiBaseUrl", releaseApiBaseUrl)
    doLast {
        val value = inputs.properties["releaseApiBaseUrl"] as String
        val endpoint = runCatching { URI(value) }.getOrNull()
        if (endpoint?.scheme != "https" || endpoint.host.isNullOrBlank() ||
            endpoint.host in setOf("localhost", "127.0.0.1", "10.0.2.2") ||
            endpoint.userInfo != null || endpoint.rawQuery != null ||
            endpoint.rawFragment != null || !value.endsWith("/")
        ) {
            throw GradleException(
                "Set NIHONIX_RELEASE_API_BASE_URL to the deployed HTTPS API URL ending in / " +
                    "(Gradle property or environment variable). Local development URLs are not release endpoints."
            )
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseEnvironment)
}

android {
    namespace = "com.nihonix.mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.nihonix.mobile"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", debugApiBaseUrl.get().asBuildConfigString())
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            buildConfigField("String", "API_BASE_URL", releaseApiBaseUrl.get().asBuildConfigString())
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Networking & Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Security & Coroutines
    implementation(libs.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}