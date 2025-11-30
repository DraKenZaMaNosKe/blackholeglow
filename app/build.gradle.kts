import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// Leer API keys desde local.properties (seguro, no se sube a GitHub)
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.secret.blackholeglow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.secret.blackholeglow"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "4.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 🔐 API Key de Gemini (se lee de local.properties)
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
    }

    // Habilitar BuildConfig
    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootProject.projectDir}/blackholeglow-release-key.jks")
            storePassword = "blackholeglow2025"
            keyAlias = "blackholeglow"
            keyPassword = "blackholeglow2025"
        }
    }

    buildTypes {
        debug {
            // 🧪 DEBUG: Ads HABILITADOS para pruebas
            buildConfigField("boolean", "ADS_ENABLED", "true")
        }
        release {
            // 🚀 RELEASE: Ads habilitados para producción
            buildConfigField("boolean", "ADS_ENABLED", "true")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)

    // Firebase BoM (Bill of Materials) - gestiona versiones automáticamente
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.config)  // Remote Config para reglas dinámicas

    // Google Sign-In
    implementation(libs.play.services.auth)

    // Google Mobile Ads (AdMob)
    implementation(libs.play.services.ads)

    // Glide para cargar imágenes (avatar del usuario)
    implementation(libs.glide)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    annotationProcessor(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}