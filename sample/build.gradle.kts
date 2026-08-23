plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.vwunofficial.ink.sample"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "io.github.vwunofficial.ink.sample.direct"
        minSdk = 26
        targetSdk = 30
        versionCode = 2
        versionName = "0.2.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":viwoods-ink"))
}
