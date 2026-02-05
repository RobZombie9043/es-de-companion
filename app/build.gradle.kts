buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

android {
    namespace = "com.esde.companion"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.esde.companion"
        minSdk = 29  // Android 10+ required
        targetSdk = 34
        versionCode = 24
        versionName = "0.5.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "recents"

    productFlavors {
        create("standard") {
            dimension = "recents"
            applicationIdSuffix = ""
            versionNameSuffix = ""
            // Standard version - shows in recents (default Android behavior)
        }

        create("persistent") {
            dimension = "recents"
            applicationIdSuffix = ""
            versionNameSuffix = "h"
            // Persistent version - hidden from recents
        }
    }

    // Configure source sets to use flavor-specific manifests
    sourceSets {
        getByName("persistent") {
            manifest.srcFile("src/main/persistent/AndroidManifest.xml")
        }
        getByName("standard") {
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                FileInputStream(keystorePropertiesFile).use { fis ->
                    keystoreProperties.load(fis)
                }

                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            } else {
                storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release-keystore.jks")
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            if (buildType.name == "release") {
                // Use the variant's versionName (which includes the suffix) instead of defaultConfig
                output.outputFileName = "ES-DE-Companion-v${versionName}.apk"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("com.caverock:androidsvg-aar:1.4")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")

    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")

    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
