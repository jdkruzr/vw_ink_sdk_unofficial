plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.vwunofficial.ink.sample"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "io.github.vwunofficial.ink.sample"
        minSdk = 26
        targetSdk = 30
        versionCode = 1
        versionName = "0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":viwoods-ink"))
}
