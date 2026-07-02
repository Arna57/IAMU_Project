plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "hr.alg.iamu_project_bp"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "hr.alg.iamu_project_bp"
        minSdk = 30
        targetSdk = 36
        compileSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    signingConfigs {
        // NOTE: credentials are committed inline ONLY because this is a
        // university project ("Signed APK app signing key setup" checklist
        // item). Never do this in a production app - use environment
        // variables, gradle.properties outside VCS, or a secrets manager.
        create("release") {
            storeFile = file("protected.keystore")
            storePassword = "iamu-release"
            keyAlias = "iamu"
            keyPassword = "iamu-release"
        }
    }

    buildTypes {
        release {
            // R8 minification/obfuscation enabled per spec ("ProGuard/R8
            // obfuscation configuration rules"). Extra keep rules go into
            // the root proguard-rules.pro (owned by the resources slice)
            // or app/src/main/keepRules/rules.keep.
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                rootProject.file("proguard-rules.pro")
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // networking (retrofit + gson)
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.converter.gson)

    // splash screen
    implementation(libs.androidx.core.splashscreen)

    // background work
    implementation(libs.androidx.work.runtime.ktx)

    // settings screen
    implementation(libs.androidx.preference.ktx)

    // ui widgets
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.fragment.ktx)

    // coroutines
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
