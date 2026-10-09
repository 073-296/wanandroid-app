plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.study.wanandroid"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.study.wanandroid"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "DEBUG", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "DEBUG", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // nav2
    implementation("androidx.navigation:navigation-compose:2.7.4")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // 序列化 JSON 库
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    // Retrofit 的序列化转换器
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // OkHttp 网络引擎 (Retrofit 底层依赖它，显式声明可以控制版本)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 日志拦截器 (调试用)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    //paging3
    val pagingVersion = "3.5.1"
    implementation("androidx.paging:paging-common:$pagingVersion")
    implementation("androidx.paging:paging-compose:$pagingVersion")

    //内嵌浏览器
    implementation("androidx.browser:browser:1.5.0")
}