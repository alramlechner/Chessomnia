import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.play.publisher)
}

// The version lives in version.properties at the repository root, so that a
// release only ever has to touch one file.
val versionProps = Properties().apply {
    file("../../version.properties").inputStream().use { load(it) }
}

android {
    namespace = "name.lechners.chessomnia"
    compileSdk = 36

    defaultConfig {
        applicationId = "name.lechners.chessomnia"
        // The app uses no Android 15 API. The hard floor would be 26 (java.time in
        // GameViewModel, and a launcher icon that exists only as anydpi-v26); 30 is
        // the comfortable floor. See ARCHITECTURE.md before lowering it.
        minSdk = 30
        // Google Play requires targetSdk 36 for new apps and updates.
        targetSdk = 36
        versionCode = versionProps.getProperty("VERSION_CODE").toInt()
        versionName = versionProps.getProperty("VERSION_NAME")
        // No abiFilters: the app has no native code of its own, but Compose pulls
        // in libandroidx.graphics.path.so. Restricting ABIs here would silently
        // exclude devices; the App Bundle lets the store generate per-ABI splits.
    }

    // Release signing is driven by an untracked keystore.properties (see
    // keystore.properties.example). Without it the release build stays unsigned,
    // which is what a fork or a CI check wants -- and it keeps every key and
    // password out of the repository.
    signingConfigs {
        val keystoreProperties = Properties().apply {
            val f = rootProject.file("keystore.properties")
            if (f.isFile) f.inputStream().use { load(it) }
        }
        if (keystoreProperties.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Lint belongs in the development loop, not in the release path: otherwise
        // lintVitalRelease runs on every single release build and is one of the most
        // expensive steps. Run it deliberately instead: ./gradlew lintRelease
        //
        // WARNING: this also suppresses the NewApi check, which is the one that would
        // catch a call above minSdk. Run lintRelease before publishing.
        checkReleaseBuilds = false
    }
}

// Publishing to Google Play. The service account key is untracked -- see
// play-service-account.json.example. Without it every ordinary task still works;
// only the publish* tasks fail, and they fail with a clear message rather than
// silently doing nothing.
//
// ⚠️ The defaults here are deliberately harmless. `publishBundle` with no further
// arguments goes to the INTERNAL track, which is a named list of at most 100
// testers -- not the store. Shipping to the public is an explicit act:
//
//     ./gradlew publishBundle --track production
//
// ⚠️ The API cannot create the very first release of an app. Google requires one
// bundle to be uploaded through the Play Console by hand before the Developer API
// will accept anything for that package.
play {
    val credentials = rootProject.file("play-service-account.json")
    if (credentials.isFile) serviceAccountCredentials.set(credentials)
    defaultToAppBundles.set(true)
    track.set("internal")
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.COMPLETED)
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}

// Deep perft runs only on request: ./gradlew test -DperftDeep=1
// Without forwarding it, the test JVM would not see the Gradle process's property.
tasks.withType<Test>().configureEach {
    System.getProperty("perftDeep")?.let { systemProperty("perftDeep", it) }
}
