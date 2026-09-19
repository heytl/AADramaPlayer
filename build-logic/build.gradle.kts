plugins {
    `kotlin-dsl`
}

group = "com.aa.duanju.buildlogic"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.3.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:1.9.22-1.0.17")
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.51.1")
}
