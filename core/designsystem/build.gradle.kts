plugins {
    id("aadrama.android.compose")
}

android.namespace = "com.aa.duanju.core.designsystem"

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
