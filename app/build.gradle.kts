import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// Read local.properties OUTSIDE the `android { }` block: inside it, `java`
// resolves to the Gradle `java` extension, not the package.
val localProperties = Properties().apply {
  val localPropFile = rootProject.file("local.properties")
  if (localPropFile.exists()) {
    localPropFile.inputStream().use { load(it) }
  }
}
val trafficApiKey = localProperties.getProperty("TRAFFIC_API_KEY") ?: ""

android {
  namespace = "com.koch.weather"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.koch.weather"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
    buildConfigField("String", "TRAFFIC_API_KEY", "\"$trafficApiKey\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug {
      isMinifyEnabled = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Network & Serialization
  implementation(libs.okhttp)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.androidx.datastore.preferences)

  // Maps & Images (No VPN / Russia friendly)
  implementation(libs.osmdroid.android)
  implementation(libs.coil.compose)

  // Local tests
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
