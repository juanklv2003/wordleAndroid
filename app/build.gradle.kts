plugins {
    alias(libs.plugins.android.application)
    // 1. ESTE ES EL PLUGIN DE KOTLIN-ANDROID
    alias(libs.plugins.kotlinAndroid)

    // 2. ¡ESTE ES EL PLUGIN DE KAPT QUE FALTABA!
    // Esta línea arregla el error "Unresolved reference: kapt"
    alias(libs.plugins.kotlinKapt)
}

android {
    namespace = "com.example.wordle"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.wordle"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11" // O '17' si tienes Java 17 instalado y quieres usarlo
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // LIBRERÍA PARA GIFS (Glide)
    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0") // <-- kapt es esencial

    implementation("com.github.madrapps:pikolo:2.0.2")




    //retrofrit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}