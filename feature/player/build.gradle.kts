plugins {
    id("aadrama.android.library")
    id("aadrama.android.hilt")
}

android.namespace = "com.aa.duanju.feature.player"

dependencies {
    implementation(project(":core:model"))
    implementation(project(":domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.material)
    implementation(libs.gsy.java)
    implementation(libs.gsy.exo2)
    implementation(libs.gsy.arm64)
    implementation(libs.gsy.armv7a)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
