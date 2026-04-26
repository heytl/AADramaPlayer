import java.util.Properties
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.aa.duanju"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aa.duanju"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // 更加健壮的 local.properties 加载逻辑
    val properties = Properties()
    val propertiesFile = project.rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use { properties.load(it) }
    }

    signingConfigs {
        create("release") {
            storeFile = file("JKS/my-release-key.jks")
            storePassword = properties.getProperty("signing.storePassword")
            keyAlias = properties.getProperty("signing.keyAlias")
            keyPassword = properties.getProperty("signing.keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 只有当本地配置存在时才启用签名，否则降级为未签名打包
            if (!properties.getProperty("signing.storePassword").isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // 自定义打包后的文件名
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    val date = SimpleDateFormat("yyyyMMdd").format(Date())
                    val baseName = "阿阿短剧_v${defaultConfig.versionName}_${date}_${variant.name}"
                    output.outputFileName.set("${baseName}.apk")
                }
            }
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/merged.kotlin_module"
            excludes += "**/kotlin_module"
            excludes += "**/merged.kotlin_module"
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // Compose
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Room
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    
    // GSYVideoPlayer
    implementation("io.github.carguo:gsyvideoplayer-java:11.0.0")
    implementation("io.github.carguo:gsyvideoplayer-exo2:11.0.0")
    implementation("io.github.carguo:gsyvideoplayer-arm64:11.0.0")
    implementation("io.github.carguo:gsyvideoplayer-armv7a:11.0.0")
    
    // View-based UI
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.11.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-video:2.6.0")
}
