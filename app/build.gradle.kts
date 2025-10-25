plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
}

android {
    namespace = "com.ssj.statuswindow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ssj.statuswindow"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = false          // 데이터바인딩 비활성
        buildConfig = true           // BuildConfig 생성
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // 모듈
    implementation(project(":data"))
    implementation(project(":core-parsing"))
    implementation(project(":core-ml"))
    implementation(project(":core-common"))

    // 버전 카탈로그 사용 (중복 피하기)
    implementation(libs.androidx.core.ktx) // 이 한 줄만 남깁니다.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.timber)

    // viewModels(), lifecycleScope 등 KTX 확장 (좌표는 Kotlin DSL 문법으로)
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation("androidx.activity:activity-ktx:1.8.0") // 버전을 libs.versions.activityKtx.get() 등으로 통일하는 것을 권장합니다.
    implementation("androidx.fragment:fragment-ktx:1.6.2") // 버전을 libs.versions.fragmentKtx.get() 등으로 통일하는 것을 권장합니다.
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // JSON 직렬화를 위한 Gson
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Room 데이터베이스
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Apache POI for Excel export
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.poi:poi-ooxml:5.2.4")


    testImplementation(kotlin("test"))
}
