import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("com.android.application")
}
val libs = the<LibrariesForLibs>()
android {
    compileSdk {
        version = release(libs.versions.android.compileSdk.get().toInt()) {
            minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
        }
    }

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // 按 ABI 分包
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }
    val signPath: String? = getenv("storyteller_f_sign_path")
    val signAlias: String? = getenv("storyteller_f_sign_alias")
    val signStorePassword: String? = getenv("storyteller_f_sign_store_password")
    val signKeyPassword: String? = getenv("storyteller_f_sign_key_password")

    signingConfigs {
        val signStorePath = signPath?.let(::File)
        if (signStorePath != null && signAlias != null && signStorePassword != null && signKeyPassword != null) {
            create("release") {
                keyAlias = signAlias
                keyPassword = signKeyPassword
                storeFile = signStorePath
                storePassword = signStorePassword
            }
        }
    }
    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/LICENSE.md",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            )
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.logging.enabled", true)
            }
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSignConfig = signingConfigs.findByName("release")
            if (releaseSignConfig != null)
                signingConfig = releaseSignConfig
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        val javaVersion = JavaVersion.forClassVersion(libs.versions.jdk.get().toInt())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    lint {
        disable.addAll(arrayOf("RememberReturnType", "UnusedMaterial3ScaffoldPaddingParameter"))
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

fun getenv(key: String): String? {
    return System.getenv(key) ?: System.getenv(key.lowercase()) ?: System.getenv(key.uppercase())
}
