plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Screenshot renders are opt-in so CI unit tests stay free of Robolectric.
val renderScreenshots = System.getenv("RENDER_SCREENSHOTS") == "1"

// Environment variables win over Gradle properties (-P / ~/.gradle/gradle.properties).
// Passwords are not trimmed: a trailing newline in a secret is a real mismatch.
fun Project.credential(name: String): String? {
    val raw = System.getenv(name) ?: (findProperty(name) as? String)
    return raw?.takeIf { it.isNotEmpty() }
}

// OpenSSL PKCS12 keystores are loaded as JKS unless storeType is pkcs12 or the
// file name ends in .p12/.pfx. The CI secret is an OpenSSL .p12, so pkcs12 is
// the default for every extension except .jks.
fun storeTypeForKeystore(storeFile: java.io.File, explicit: String?): String {
    explicit?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { return it }
    return when (storeFile.extension.lowercase()) {
        "jks" -> "jks"
        else -> "pkcs12"
    }
}

fun Project.resolveStoreFile(rawPath: String): java.io.File {
    val expanded = when {
        rawPath == "~" -> System.getProperty("user.home")
        rawPath.startsWith("~/") -> System.getProperty("user.home") + rawPath.removePrefix("~")
        else -> rawPath
    }
    return file(expanded)
}

val signingKeystorePath = credential("SIGNING_KEYSTORE_PATH")
val signingStorePassword = credential("SIGNING_STORE_PASSWORD")
val signingKeyAlias = credential("SIGNING_KEY_ALIAS")
val signingKeyPassword = credential("SIGNING_KEY_PASSWORD")
val signingStoreType = credential("SIGNING_STORE_TYPE")?.trim()?.takeIf { it.isNotEmpty() }
val requireReleaseSigning = credential("REQUIRE_RELEASE_SIGNING")
    ?.equals("true", ignoreCase = true) == true

val requiredSigning = listOf(
    "SIGNING_KEYSTORE_PATH" to signingKeystorePath,
    "SIGNING_STORE_PASSWORD" to signingStorePassword,
    "SIGNING_KEY_ALIAS" to signingKeyAlias,
    "SIGNING_KEY_PASSWORD" to signingKeyPassword,
)
val presentSigning = requiredSigning.filter { it.second != null }
if (presentSigning.isNotEmpty() && presentSigning.size != requiredSigning.size) {
    val missing = requiredSigning.filter { it.second == null }.joinToString(", ") { it.first }
    throw GradleException(
        "Incomplete APK signing configuration. Missing $missing. " +
            "Set SIGNING_KEYSTORE_PATH, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, and " +
            "SIGNING_KEY_PASSWORD, or leave all of them unset."
    )
}

val signingKeystore = signingKeystorePath?.let { rawPath ->
    resolveStoreFile(rawPath).also { storeFile ->
        if (!storeFile.isFile) {
            throw GradleException(
                "SIGNING_KEYSTORE_PATH does not exist or is not a file: ${storeFile.path}"
            )
        }
    }
}

if (requireReleaseSigning && signingKeystore == null) {
    throw GradleException(
        "Release signing is required (REQUIRE_RELEASE_SIGNING=true) but no signing " +
            "credentials were provided. Refusing to build an unsigned release APK. Set " +
            "SIGNING_KEYSTORE_PATH, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, and " +
            "SIGNING_KEY_PASSWORD."
    )
}

val resolvedStoreType = signingKeystore?.let { storeTypeForKeystore(it, signingStoreType) }
if (signingKeystore != null) {
    logger.lifecycle(
        "Signing debug and release APKs with alias '$signingKeyAlias' (storeType=$resolvedStoreType)."
    )
}

android {
    namespace = "com.ekhtiari.smsorganizer"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.ekhtiari.smsorganizer"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "1.0.9"
        resourceConfigurations += "en"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signature schemes stay at AGP defaults (v2/v3 for this minSdk, v1 when AGP enables it).
    // Locals so the signingConfigs lambda can use non-null values.
    val keystore = signingKeystore
    val storePasswordValue = signingStorePassword
    val keyAliasValue = signingKeyAlias
    val keyPasswordValue = signingKeyPassword
    val storeTypeValue = resolvedStoreType
    val releaseSigning = if (
        keystore != null &&
        storePasswordValue != null &&
        keyAliasValue != null &&
        keyPasswordValue != null &&
        storeTypeValue != null
    ) {
        signingConfigs.create("release") {
            storeFile = keystore
            storePassword = storePasswordValue
            keyAlias = keyAliasValue
            keyPassword = keyPasswordValue
            storeType = storeTypeValue
        }
    } else {
        null
    }

    buildTypes {
        release {
            if (releaseSigning != null) {
                signingConfig = releaseSigning
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Same stable key as release when credentials exist, so CI debug APKs
            // update each other. The .debug application id keeps them beside release.
            // Without credentials, AGP's default debug keystore is used.
            if (releaseSigning != null) {
                signingConfig = releaseSigning
            }
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = renderScreenshots
        unitTests.all {
            if (renderScreenshots) {
                it.systemProperty("robolectric.graphicsMode", "NATIVE")
                it.systemProperty("robolectric.pixelCopyMode", "hardware")
                it.maxHeapSize = "4096m"
            }
        }
    }

    if (renderScreenshots) {
        sourceSets.getByName("test").java.srcDir("src/screenshot/java")
    }

    // APK file name: SMS-Organizer-<versionName>.apk (debug includes the -debug suffix)
    applicationVariants.configureEach {
        outputs.configureEach {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "SMS-Organizer-${versionName}.apk"
        }
    }
}

dependencies {
    // Compose 1.8.2. material3 is pinned below, outside this BOM.
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // Expressive APIs (MaterialExpressiveTheme, MotionScheme, ButtonGroup, FloatingToolbar,
    // LoadingIndicator, MaterialShapes). 1.5.0-alpha12 is the newest line that still
    // builds with Kotlin 2.0.21 and Compose 1.8; 1.5.0 stable needs Kotlin 2.2.
    implementation("androidx.compose.material3:material3:1.5.0-alpha12")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    if (renderScreenshots) {
        testImplementation("org.robolectric:robolectric:4.14.1")
        testImplementation("androidx.compose.ui:ui-test-junit4")
        // Registers ComponentActivity so createComposeRule can launch it.
        // Only on the classpath when screenshots are rendered, so CI APKs stay unchanged.
        debugImplementation("androidx.compose.ui:ui-test-manifest")
    }
}
