import com.android.build.api.variant.FilterConfiguration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.adnan.memoracloud"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.adnan.memoracloud"
        minSdk = 26
        targetSdk = 37
        // The version catalog is the source of truth for local builds. CI overrides both values
        // from the release workflow so versionCode keeps increasing without editing files.
        versionCode = providers.gradleProperty("appVersionCode").orNull?.toIntOrNull()
            ?: libs.versions.appVersionCode.get().toInt()
        versionName = providers.gradleProperty("appVersionName").orNull?.takeIf { it.isNotBlank() }
            ?: libs.versions.appVersion.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Architecture packaging:
    //   (default)                                 -> one APK per ABI plus a universal APK
    //   -PsplitAbis=true -Pabis=arm64-v8a         -> a single ~28 MB APK for modern phones
    //   -PsplitAbis=false                        -> one universal APK (~98 MB, all ABIs inside)
    // Every ABI carries its own copy of the TDLib native library, which is why the universal
    // APK is roughly four times the size of a single-architecture one.
    val requestedAbis = providers.gradleProperty("abis").orNull
        ?.split(',')
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty()
    val splitAbis = providers.gradleProperty("splitAbis").orNull?.toBoolean() ?: true
    splits {
        abi {
            isEnable = splitAbis
            reset()
            include(
                *(requestedAbis.ifEmpty {
                    listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                }).toTypedArray()
            )
            isUniversalApk = splitAbis && requestedAbis.isEmpty()
        }
    }

    val ciKeystore = System.getenv("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
    signingConfigs {
        create("release") {
            if (ciKeystore != null) {
                storeFile = file(ciKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        debug {
            if (ciKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (ciKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs {
            // false (default): native libraries stored uncompressed, so the app starts faster.
            // -PlegacyJniPackaging compresses them instead, which makes the APK roughly 25-30%
            // smaller to download in exchange for extracting them once at install time.
            useLegacyPackaging = providers.gradleProperty("legacyJniPackaging").isPresent
        }
    }
}

@Suppress("UnstableApiUsage")
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val abi = output.filters
                .find { it.filterType == FilterConfiguration.FilterType.ABI }
                ?.identifier
                ?: "universal"
            val extension = output.outputFileName.get().substringAfterLast('.')
            output.outputFileName.set(
                output.versionName.map { version ->
                    "${rootProject.name}-$version-$abi-${variant.buildType}.$extension"
                }
            )
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":ui"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.google.android.material)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.androidx.workmanager)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.runtime)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource)
    if (providers.gradleProperty("withFfmpegDecoder").isPresent) {
        implementation(libs.androidx.media3.decoder.ffmpeg)
    }

    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.telephoto.zoomable.coil)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.exifinterface)
    implementation(libs.zip4j)
    implementation(libs.zxing.core)
    implementation(libs.tdlib)

    testImplementation(libs.junit)
    testImplementation(libs.koin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
