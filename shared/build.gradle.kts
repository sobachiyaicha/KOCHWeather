import java.util.Properties

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

// TRAFFIC_API_KEY из local.properties (как раньше делал buildConfigField в :app).
// Читается ВНЕ android { }: внутри него `java` резолвится в gradle-расширение.
val localProperties = Properties().apply {
  val localPropFile = rootProject.file("local.properties")
  if (localPropFile.exists()) {
    localPropFile.inputStream().use { load(it) }
  }
}
val trafficApiKey = localProperties.getProperty("TRAFFIC_API_KEY") ?: ""

// Генерируем com.koch.weather.BuildConfig в androidMain shared: buildFeatures.buildConfig
// недоступен в androidLibrary-DSL KMP-библиотеки, а namespace com.koch.weather занят
// R-классом. TrafficRepository/MainScreen читают его без изменений содержимого.
// Отличия от прежнего App BuildConfig: DEBUG = false (FPS-оверлей — debug-only
// диагностика; port plan §4 допускает её заглушку). Phase 2 заменит на
// expect/actual trafficApiKey()/isDebugBuild() (handoff §5).
val kochBuildConfigDir = layout.buildDirectory.dir("generated/kochBuildConfig/kotlin")
val generateKochBuildConfig = tasks.register("generateKochBuildConfig") {
  val outDir = kochBuildConfigDir
  val apiKey = trafficApiKey
  outputs.dir(outDir)
  doLast {
    val dir = outDir.get().asFile.resolve("com/koch/weather")
    dir.mkdirs()
    dir.resolve("BuildConfig.kt").writeText(
      """
      |package com.koch.weather
      |
      |// AUTO-GENERATED задачей :shared:generateKochBuildConfig — НЕ править руками.
      |// TRAFFIC_API_KEY берётся из local.properties (как buildConfigField в :app).
      |// DEBUG = false: KMP-библиотека собирается одной компиляцией, per-variant
      |// значение недоступно; FPS-оверлей (debug-only) поэтому скрыт — port plan §4.
      |// Phase 2 (data-porter) заменит на expect/actual trafficApiKey()/isDebugBuild().
      |object BuildConfig {
      |    const val TRAFFIC_API_KEY: String = "$apiKey"
      |    const val DEBUG: Boolean = false
      |}
      """.trimMargin()
    )
  }
}

kotlin {
  // AGP 9: android target и его namespace/SDK настраиваются плагином
  // com.android.kotlin.multiplatform.library прямо здесь (DSL проверен
  // эталонным скриптом /tmp/dslprobe: блок `android { }` внутри kotlin { }).
  android {
    namespace = "com.koch.weather"
    compileSdk = 36
    minSdk = 24

    // Ресурсы androidMain (drawable-nodpi/raw/font для R-класса) —
    // без этого флага AGP не включает res-пайплайн KMP-библиотеки
    // (sources.res == null, R не генерируется).
    androidResources { enable = true }
  }

  iosArm64 {
    binaries.framework {
      baseName = "Shared"
      isStatic = false
    }
  }
  iosSimulatorArm64 {
    binaries.framework {
      baseName = "Shared"
      isStatic = false
    }
  }

  sourceSets {
    commonMain.dependencies {
      // Прямые координаты вместо deprecated-аксессоров compose.* (CMP 1.12.2).
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.compose.material.icons.extended)
      implementation(libs.compose.ui)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.serialization.json)
    }
    androidMain {
      kotlin.srcDir(kochBuildConfigDir)
      dependencies {
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.androidx.lifecycle.viewmodel.compose)
        implementation(libs.androidx.lifecycle.process)
        implementation(libs.androidx.datastore.preferences)
        implementation(libs.okhttp)
        implementation(libs.osmdroid.android)
        implementation(libs.coil.compose)
      }
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
  }
}

tasks.configureEach {
  if (name == "compileAndroidMain" || name == "compileDebugAndroidMain" || name == "compileReleaseAndroidMain") {
    dependsOn(generateKochBuildConfig)
  }
}
