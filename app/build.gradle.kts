plugins {
    id("com.android.application")
}

android {
    namespace = "com.soyache.blurgiro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.soyache.blurgiro"
        minSdk = 29
        targetSdk = 35
        versionCode = 7
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    flavorDimensions += "api"
    productFlavors {
        create("compat") {
            dimension = "api"
            minSdk = 29
            buildConfigField("String", "FLAVOR_LABEL", "\"Android 10–13\"")
        }
        create("standard") {
            dimension = "api"
            minSdk = 34
            buildConfigField("String", "FLAVOR_LABEL", "\"Android 14+\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":motion"))
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("androidx.annotation:annotation:1.9.1")
    testImplementation("junit:junit:4.13.2")
}
