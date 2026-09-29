import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    id("aadrama.android.application")
    id("aadrama.android.hilt")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.aa.duanju"

    defaultConfig {
        applicationId = "com.aa.duanju"
        versionCode = 3
        versionName = "1.0.1"
    }

    val properties = Properties()
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use(properties::load)

    signingConfigs {
        create("release") {
            storeFile = file("JKS/my-release-key.jks")
            storePassword = properties.getProperty("signing.storePassword")
            keyAlias = properties.getProperty("signing.keyAlias")
            keyPassword = properties.getProperty("signing.keyPassword")
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "BENCHMARK", "false")
        }
        release {
            buildConfigField("boolean", "BENCHMARK", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (!properties.getProperty("signing.storePassword").isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        create("benchmark") {
            initWith(getByName("release"))
            buildConfigField("boolean", "BENCHMARK", "true")
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    androidComponents.onVariants { variant ->
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                val date = SimpleDateFormat("yyyyMMdd").format(Date())
                output.outputFileName.set("阿阿短剧_v${defaultConfig.versionName}_${date}_${variant.name}.apk")
            }
        }
    }
}

dependencies {
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":core:database"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:library"))
    implementation(project(":feature:player"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.coil.compose)
    implementation(libs.hilt.android)
    implementation(libs.androidx.profileinstaller)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    baselineProfile(project(":benchmark"))
}
