plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dagger.hilt)
    // Required for Data Binding to discover Kotlin @BindingAdapter / @BindingConversion.
    // Hilt remains on KSP only — do not add kapt(hilt.compiler).
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.navigation.safe.args)
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.google.firebase.firebase.perf)
    id("kotlin-parcelize")
}
hilt {
    enableAggregatingTask = false
}
ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}
kapt {
    correctErrorTypes = true
}

// Next-Gen SDK embeds GMA classes; mediation adapters still pull legacy play-services-ads.
// Exclude legacy modules globally to avoid checkDebugDuplicateClasses failures.
// https://developers.google.com/admob/android/next-gen/mediation
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}

android {
    namespace = "com.aiface.aging"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "ai.faceaging.aiphoto.generator.editor"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        setProperty("archivesBaseName", "Ai Enhancer-" + versionName)
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }
    }

    buildTypes {

        buildTypes {
            debug {
                manifestPlaceholders.put("admob_app_id", "ca-app-pub-3940256099942544~3347511713")

//                buildConfigField("String", "inter_splash", "\"ca-app-pub-5972202469838280/1234063336\"")
//                buildConfigField("String", "inter_splash_high", "\"ca-app-pub-5972202469838280/1757978372\"")
//
//
//                buildConfigField("String", "native_splash", "\"ca-app-pub-5972202469838280/3766441452\"")
//                buildConfigField("String", "banner_splash", "\"ca-app-pub-5972202469838280/4950242217\"")
//                buildConfigField("String", "native_splash_high", "\"ca-app-pub-5972202469838280/3766441452\"")

                buildConfigField("String", "inter_splash", "\"ca-app-pub-3940256099942544/1033173712\"")
                buildConfigField("String", "inter_splash_high", "\"ca-app-pub-3940256099942544/1033173712\"")

                buildConfigField("String", "banner_splash", "\"ca-app-pub-3940256099942544/6300978111\"")
                buildConfigField("String", "native_splash", "\"ca-app-pub-3940256099942544/2247696110\"")
                buildConfigField("String", "native_splash_high", "\"ca-app-pub-3940256099942544/2247696110\"")



                resValue("string", "native_ob1", "ca-app-pub-3940256099942544/2247696110")
                resValue("string", "native_ob1_high", "ca-app-pub-3940256099942544/2247696110")
                resValue("string", "native_ob3", "ca-app-pub-3940256099942544/2247696110")
                resValue("string", "native_ob3_high", "ca-app-pub-3940256099942544/2247696110")
                resValue("string", "native_ob4", "ca-app-pub-3940256099942544/2247696110")
                resValue("string", "native_ob4_high", "ca-app-pub-3940256099942544/2247696110")
                resValue("string", "native_full_ob1", "ca-app-pub-3940256099942544/2247696110")
                resValue("string", "native_full_ob1_high", "ca-app-pub-3940256099942544/2247696110")
                resValue("string", "native_full_ob2", "ca-app-pub-3940256099942544/2247696110")
                resValue("string", "native_full_ob2_high", "ca-app-pub-3940256099942544/2247696110")

                buildConfigField("String", "inter_survey", "\"ca-app-pub-3940256099942544/1033173712\"")
                buildConfigField("String", "inter_survey_high", "\"ca-app-pub-3940256099942544/1033173712\"")

                buildConfigField("String", "native_preview", "\"ca-app-pub-3940256099942544/2247696110\"")
                buildConfigField("String", "native_preview_hf", "\"ca-app-pub-3940256099942544/2247696110\"")

                buildConfigField("String", "native_survey", "\"ca-app-pub-3940256099942544/2247696110\"")
                buildConfigField("String", "native_survey_hf", "\"ca-app-pub-3940256099942544/2247696110\"")

                buildConfigField("String", "inter_ob", "\"ca-app-pub-3940256099942544/1033173712\"")
                buildConfigField("String", "inter_ob_high", "\"ca-app-pub-3940256099942544/1033173712\"")

                buildConfigField("String", "native_permission", "\"ca-app-pub-3940256099942544/2247696110\"")
                buildConfigField("String", "native_permission_hf", "\"ca-app-pub-3940256099942544/2247696110\"")

                buildConfigField("String", "native_home", "\"ca-app-pub-3940256099942544/2247696110\"")
                buildConfigField("String", "native_home_hf", "\"ca-app-pub-3940256099942544/2247696110\"")

                buildConfigField("String", "inter_home", "\"ca-app-pub-3940256099942544/1033173712\"")
                buildConfigField("String", "inter_home_high", "\"ca-app-pub-3940256099942544/1033173712\"")

                buildConfigField("String", "banner_home", "\"ca-app-pub-3940256099942544/6300978111\"")
                buildConfigField("String", "banner_home_high", "\"ca-app-pub-3940256099942544/6300978111\"")

                buildConfigField("String", "inter_collage", "\"ca-app-pub-3940256099942544/1033173712\"")
                buildConfigField("String", "inter_collage_hf", "\"ca-app-pub-3940256099942544/1033173712\"")

                buildConfigField("String", "native_share", "\"ca-app-pub-3940256099942544/2247696110\"")
                buildConfigField("String", "native_share_hf", "\"ca-app-pub-3940256099942544/2247696110\"")

//                buildConfigField("String", "native_language", "\"ca-app-pub-3940256099942544/2247696110\"")
//                buildConfigField("String", "native_language_high", "\"ca-app-pub-3940256099942544/2247696110\"")
//
//                buildConfigField("String", "native_language_alt", "\"ca-app-pub-3940256099942544/2247696110\"")
//                buildConfigField("String", "native_language_alt_high", "\"ca-app-pub-3940256099942544/2247696110\"")
                buildConfigField("String", "native_language", "\"ca-app-pub-3940256099942544/2247696110\"")
                buildConfigField("String", "native_language_high", "\"ca-app-pub-3940256099942544/2247696110\"")

                buildConfigField("String", "native_language_alt", "\"ca-app-pub-3940256099942544/2247696110\"")
                buildConfigField("String", "native_language_alt_high", "\"ca-app-pub-3940256099942544/2247696110\"")

                buildConfigField("String", "native_collage", "\"ca-app-pub-3940256099942544/2247696110\"")
                buildConfigField("String", "native_collage_hf", "\"ca-app-pub-3940256099942544/2247696110\"")

                buildConfigField("String", "app_open_resume", "\"ca-app-pub-3940256099942544/9257395921\"")

                buildConfigField("String", "reward_home", "\"ca-app-pub-3940256099942544/5224354917\"")
                buildConfigField("String", "reward_home_hf", "\"ca-app-pub-3940256099942544/5224354917\"")
            }
            release {
                manifestPlaceholders.put("admob_app_id", "ca-app-pub-5972202469838280~6432133240")


                buildConfigField("String", "inter_splash", "\"ca-app-pub-5972202469838280/3958013142\"")
                buildConfigField("String", "inter_splash_high", "\"ca-app-pub-5972202469838280/3958013142\"")


                buildConfigField("String", "native_splash", "\"ca-app-pub-5972202469838280/3766441452\"")
                buildConfigField("String", "banner_splash", "\"ca-app-pub-5972202469838280/5271094817\"")
                buildConfigField("String", "native_splash_high", "\"ca-app-pub-5972202469838280/3766441452\"")




                resValue("string", "native_ob1", "ca-app-pub-5972202469838280/3766441452")
                resValue("string", "native_ob1_high", "ca-app-pub-5972202469838280/3766441452")
                resValue("string", "native_full_ob1", "ca-app-pub-5972202469838280/3766441452")
                resValue("string", "native_full_ob1_high", "ca-app-pub-5972202469838280/3766441452")
                resValue("string", "native_full_ob2", "ca-app-pub-5972202469838280/3766441452")
                resValue("string", "native_full_ob2_high", "ca-app-pub-5972202469838280/3766441452")
                resValue("string", "native_ob3", "ca-app-pub-5972202469838280/3766441452")
                resValue("string", "native_ob3_high", "ca-app-pub-5972202469838280/3766441452")
                resValue("string", "native_ob4", "ca-app-pub-5972202469838280/3766441452")
                resValue("string", "native_ob4_high", "ca-app-pub-5972202469838280/3766441452")

                buildConfigField("String", "inter_survey", "\"ca-app-pub-5972202469838280/3958013142\"")
                buildConfigField("String", "inter_survey_high", "\"ca-app-pub-5972202469838280/3958013142\"")

                buildConfigField("String", "native_survey", "\"ca-app-pub-5972202469838280/3766441452\"")
                buildConfigField("String", "native_survey_hf", "\"ca-app-pub-5972202469838280/3766441452\"")

                buildConfigField("String", "native_preview", "\"ca-app-pub-5972202469838280/3214797175\"")
                buildConfigField("String", "native_preview_hf", "\"ca-app-pub-5972202469838280/3214797175\"")

                buildConfigField("String", "inter_ob", "\"ca-app-pub-5972202469838280/1234063336\"")
                buildConfigField("String", "inter_ob_high", "\"ca-app-pub-5972202469838280/1757978372\"")

                buildConfigField("String", "native_permission", "\"ca-app-pub-5972202469838280/3214797175\"")
                buildConfigField("String", "native_permission_hf", "\"ca-app-pub-5972202469838280/3214797175\"")

                buildConfigField("String", "native_home", "\"ca-app-pub-5972202469838280/3214797175\"")
                buildConfigField("String", "native_home_hf", "\"ca-app-pub-5972202469838280/3214797175\"")

                buildConfigField("String", "inter_home", "\"ca-app-pub-5972202469838280/1591438326\"")
                buildConfigField("String", "inter_home_high", "\"ca-app-pub-5972202469838280/1591438326\"")

                buildConfigField("String", "banner_home", "\"ca-app-pub-5972202469838280/5271094817\"")
                buildConfigField("String", "banner_home_high", "\"ca-app-pub-5972202469838280/5271094817\"")

                buildConfigField("String", "inter_collage", "\"ca-app-pub-5972202469838280/1591438326\"")
                buildConfigField("String", "inter_collage_hf", "\"ca-app-pub-5972202469838280/1591438326\"")

                buildConfigField("String", "native_share", "\"ca-app-pub-5972202469838280/3214797175\"")
                buildConfigField("String", "native_share_hf", "\"ca-app-pub-5972202469838280/3214797175\"")

                buildConfigField("String", "native_language", "\"ca-app-pub-5972202469838280/2688897573\"")
                buildConfigField("String", "native_language_high", "\"ca-app-pub-5972202469838280/2688897573\"")

                buildConfigField("String", "native_language_alt", "\"ca-app-pub-5972202469838280/2688897573\"")
                buildConfigField("String", "native_language_alt_high", "\"ca-app-pub-5972202469838280/2688897573\"")

                buildConfigField("String", "native_collage", "\"ca-app-pub-5972202469838280/3214797175\"")
                buildConfigField("String", "native_collage_hf", "\"ca-app-pub-5972202469838280/3214797175\"")

                buildConfigField("String", "app_open_resume", "\"ca-app-pub-5972202469838280/7965274988\"")

                buildConfigField("String", "reward_home", "\"ca-app-pub-5972202469838280/9278356658\"")
                buildConfigField("String", "reward_home_hf", "\"ca-app-pub-5972202469838280/9278356658\"")

                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )



            }

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
        viewBinding = true
        dataBinding = true
        buildConfig = true
        compose = true
    }
}

dependencies {
    // :cameraview excluded — Ai Enhancer is gallery-only.
    implementation(project(":sticker"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(mapOf("name" to "gpu_img", "ext" to "aar"))

    //InAppBilling
    implementation("com.android.billingclient:billing-ktx:8.0.0")

    implementation(libs.sdp)
    implementation(libs.ssp)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.camera.video)

    // Glide (legacy screens). Catalog thumbs use Coil+OkHttp via TemplateThumbLoader.
    implementation(libs.bumptech.glide)

    implementation(libs.easypermissions)

    //Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    implementation("io.github.ParkSangGwon:tedonactivityresult-rx2:1.0.10") {
        exclude(group = "com.android.support", module = "support-compat")
    }
//Wait Dialogue
    implementation("com.github.harrisonsj:KProgressHUD:1.1")

    implementation("com.google.code.gson:gson:2.9.0")
    //implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    //image filters
    implementation("org.wysaid:gpuimage-plus:3.2.0-16k-min")

    //seekbar
    implementation("com.github.koliong:BubbleSeekBar:2.1.1")
    implementation("com.github.rtugeek:colorseekbar:1.7.3")

    implementation(libs.dagger.hilt)
    ksp(libs.hilt.compiler)
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.0")
   // kapt(libs.hilt.compiler)

    implementation(libs.arcseekbar)


    implementation(libs.datastore.preference)
    implementation(libs.datastore.core)

   /* implementation(libs.playservices.ads)
    //mediation adapters
    implementation(libs.vungle)
    implementation(libs.fyber)

    implementation(libs.facebook)
    implementation(libs.inmobi)
    implementation(libs.mintegral)
    implementation(libs.applovin)*/
    implementation(libs.ads.mobile.sdk)

    implementation(libs.vungle)
    implementation(libs.fyber)
    implementation(libs.facebook)
    implementation(libs.inmobi)
    implementation(libs.mintegral)
    implementation(libs.applovin)
    implementation(libs.pangle)

    implementation(libs.shimmer)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.adjust.android)

    //Lottie
    implementation("com.airbnb.android:lottie:4.2.0")
    implementation(libs.facebook.android.sdk)

    implementation(libs.discretescrollview)

    implementation("androidx.work:work-runtime:2.9.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation(libs.chip.navigation.bar)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Chucker (network inspector)
    debugImplementation("com.github.chuckerteam.chucker:library:4.3.0")
    releaseImplementation("com.github.chuckerteam.chucker:library-no-op:4.3.0")

    // Image loading
    implementation(libs.coil)
    implementation(libs.coil.gif)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    //ml-kit selfie segmentation
    implementation("com.google.android.gms:play-services-mlkit-subject-segmentation:16.0.0-beta1")

    implementation(libs.mediapipe.vision)
    implementation(libs.ambilwarna)
    implementation(libs.android.image.cropper)

    // Compose + Haze glass shell (MainFragment bottom nav).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.backdrop)
    debugImplementation(libs.androidx.compose.ui.tooling)
}