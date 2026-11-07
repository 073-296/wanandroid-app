import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    alias(libs.plugins.ksp)
}

// 签名信息放在 keystore.properties（已在 .gitignore 中忽略），
// 文件缺失时自动跳过签名配置，保证他人 clone 后仍能正常构建 debug
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropsFile.exists()

android {
    namespace = "com.study.wanandroid"
    compileSdk = 35

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.study.wanandroid"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            //创建日志拦截器所需
            buildConfigField("boolean", "DEBUG", "true")
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            //创建日志拦截器所需
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
    ksp{
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    // Retrofit 的序列化转换器
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.12.0")

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

    //Coil
    implementation("io.coil-kt:coil-compose:2.4.0")

    //Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)

    // WorkManager (Kotlin + Coroutines)
    implementation("androidx.work:work-runtime-ktx:2.12.0")
}