plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.yosea.skripsi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.yosea.skripsi"
        minSdk = 35 // Catatan: minSdk 35 ini sangat tinggi (Android 15+).
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
        // PERBAIKAN: Diubah ke 1.8 untuk standar kompatibilitas Android
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        // PERBAIKAN: Diubah ke "1.8" untuk standar kompatibilitas Android
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        mlModelBinding = true
    }
    sourceSets {
        getByName("main") {
            assets {
                // PERBAIKAN: Mengarahkan ke folder 'ml' agar terbaca sebagai aset
                // Saya juga memperbaiki path agar menggunakan '/'
                srcDirs("src/main/assets", "src/main/ml")
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // PERBAIKAN: Anda perlu library TFLite dasar untuk memuat model secara manual
    // Saya tambahkan versi stabil terbaru
    implementation("org.tensorflow:tensorflow-lite:2.16.1")

    // Ini sudah ada di libs.versions.toml Anda dan PENTING untuk proses gambar
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)

    // PERBAIKAN: 'libs.tensorflow.lite.gpu' tidak ada di TOML Anda.
    // Saya tambahkan library GPU delegate secara manual
    implementation("org.tensorflow:tensorflow-lite-gpu:2.16.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.compose.material3:material3")
}