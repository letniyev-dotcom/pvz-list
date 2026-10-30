plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ru.pvzlist.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.pvzlist.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // TDLib тянет нативные библиотеки: оставляем только реальные ABI телефонов
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    // Релизный ключ лежит в app/release.jks
    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storeType = "pkcs12"
            storePassword = "eBgmBsWAEcoOME55QFCO"
            keyAlias = "pvz"
            keyPassword = "eBgmBsWAEcoOME55QFCO"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// Только release: debug-вариант отключён
androidComponents {
    beforeVariants(selector().withBuildType("debug")) { variant ->
        variant.enable = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation-core")
    // Настоящий вход и отправка через Telegram (TDLib)
    implementation("com.github.tdlibx:td:1.8.56")
}
