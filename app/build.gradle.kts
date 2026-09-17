plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services) apply false
}

android {
    namespace = "com.faskn.composeplayground"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.faskn.composeplayground"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

composeCompiler {
    if (project.hasProperty("enableComposeCompilerReports")) {
        reportsDestination = layout.buildDirectory.dir("compose_metrics")
        metricsDestination = layout.buildDirectory.dir("compose_metrics")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Dependency constraints to resolve Ktor conflicts efficiently
    constraints {
        implementation("io.ktor:ktor-client-core:2.3.12") {
            because("Forcing a stable Ktor version to resolve transitive conflicts")
        }
    }

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sceneview)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.vision.common)
    implementation(libs.play.services.mlkit.subject.segmentation)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.generativeai)

    implementation(libs.firebase.app.check)
    implementation(libs.firebase.app.check.debug)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Apply google-services plugin only if google-services.json exists
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
