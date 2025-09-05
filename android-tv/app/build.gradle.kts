plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.moontv.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.moontv.tv"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    // CI 可选签名：通过 -PTV_* 或环境变量注入
    signingConfigs {
        create("ci") {
            val storeFileProp = (project.findProperty("TV_STORE_FILE") as String?) ?: System.getenv("TV_STORE_FILE")
            val storePass = (project.findProperty("TV_STORE_PASSWORD") as String?) ?: System.getenv("TV_STORE_PASSWORD")
            val keyAliasProp = (project.findProperty("TV_KEY_ALIAS") as String?) ?: System.getenv("TV_KEY_ALIAS")
            val keyPass = (project.findProperty("TV_KEY_PASSWORD") as String?) ?: System.getenv("TV_KEY_PASSWORD")
            if (storeFileProp != null && storePass != null && keyAliasProp != null && keyPass != null) {
                storeFile = file(storeFileProp)
                storePassword = storePass
                keyAlias = keyAliasProp
                keyPassword = keyPass
            }
        }
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.11" }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

    buildTypes {
        getByName("release") {
            // 如注入了签名参数则启用 CI 签名
            val hasSign = (project.findProperty("TV_STORE_FILE") as String?) != null || System.getenv("TV_STORE_FILE") != null
            if (hasSign) {
                signingConfig = signingConfigs.getByName("ci")
            }
            isMinifyEnabled = false
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.foundation:foundation")

    // TV Compose
    implementation("androidx.tv:tv-foundation:1.0.0-alpha10")
    implementation("androidx.tv:tv-material:1.0.0-alpha10")

    // Networking placeholders (to be wired in M2)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ExoPlayer (播放器)
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")

    // Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Images
    implementation("io.coil-kt:coil-compose:2.6.0")
}
