import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.spendr.app.kt"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.spendr.app.kt"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "0.4.1"
    }

    val visionEnv = rootProject.file("../vision-api/.env")
    val visionToken = if (visionEnv.isFile) {
        visionEnv.readLines().firstOrNull { it.startsWith("API_TOKEN=") }
            ?.substringAfter('=')?.trim()?.trim('"', '\'').orEmpty()
    } else ""
    fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    flavorDimensions += "distribution"
    productFlavors {
        create("mainApp") {
            dimension = "distribution"
            manifestPlaceholders["deepLinkScheme"] = "spendrkt"
            buildConfigField("String", "DEEP_LINK_SCHEME", "\"spendrkt\"")
            buildConfigField("String", "VISION_API_TOKEN", "\"\"")
            buildConfigField("String", "VISION_API_URL", "\"\"")
        }
        create("dev") {
            dimension = "distribution"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            manifestPlaceholders["deepLinkScheme"] = "spendrktdev"
            buildConfigField("String", "DEEP_LINK_SCHEME", "\"spendrktdev\"")
            buildConfigField("String", "VISION_API_TOKEN", quoted(visionToken))
            buildConfigField("String", "VISION_API_URL", quoted("https://vision-api.ts.uzuki-p.my.id"))
        }
    }

// Release signing: a user-provided keystore via keystore.properties at the repo
// root (git-ignored). When absent, release builds fall back to debug signing so
// `assembleRelease` still produces an installable artifact for local testing.
    val keystoreProperties = Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) file.inputStream().use(::load)
    }
    val releaseSigning = keystoreProperties["storeFile"]?.let { storeFilePath ->
        signingConfigs.create("release") {
            storeFile = rootProject.file(storeFilePath)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseConfig = releaseSigning
            if (releaseConfig != null) {
                signingConfig = releaseConfig
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.material.icons.extended)
    implementation(libs.material.kolor)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
