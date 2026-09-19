plugins {
    id("aadrama.android.library")
    id("aadrama.android.hilt")
}

android.namespace = "com.aa.duanju.core.media"

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
}
