import org.gradle.api.artifacts.component.ModuleComponentSelector

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.kotlin.multiplatform.library) apply false
  alias(libs.plugins.compose.compiler) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.compose.multiplatform) apply false
}

// Android-варианты артефактов Compose Multiplatform (org.jetbrains.compose.*) —
// пустые shim-модули, транзитивно тянущие androidx.compose 1.12.1 (CMP 1.12.2),
// а тот требует AGP 9.1.0 (AAR metadata minAgpVersion). Проект зафиксирован
// на AGP 9.0.1, поэтому на Android-конфигурациях CMP-координаты подменяются
// на androidx с теми же версиями, что давал BOM 2026.03.01 до реструктуризации
// (ui/runtime/foundation/animation 1.10.6, material3 1.4.0, icons 1.7.8).
// iOS-конфигурации продолжают использовать полноценные артефакты CMP 1.12.2.
// Список правил = граф CMP android-вариантов (по POM/module-metadata в кэше).
subprojects {
  configurations.configureEach {
    // Подменяем только в Android-резолюциях: KMP-конфиги shared начинаются с
    // "android", конфиги вариантов androidApp — с debug/release/benchmark/test.
    // iOS/metadata/native конфигурации (ios*, metadata*, apple*, native*) не трогаем.
    val isAndroidResolution = name.startsWith("android") ||
      name.startsWith("debug") || name.startsWith("release") ||
      name.startsWith("benchmark") || name.startsWith("test")
    if (!isAndroidResolution) return@configureEach
    resolutionStrategy.dependencySubstitution {
      val rules: List<Pair<String, String>> = listOf(
        // runtime
        "org.jetbrains.compose.runtime:runtime" to "androidx.compose.runtime:runtime:1.10.6",
        "org.jetbrains.compose.runtime:runtime-android" to "androidx.compose.runtime:runtime-android:1.10.6",
        "org.jetbrains.compose.runtime:runtime-saveable" to "androidx.compose.runtime:runtime-saveable:1.10.6",
        "org.jetbrains.compose.runtime:runtime-saveable-android" to "androidx.compose.runtime:runtime-saveable-android:1.10.6",
        // ui
        "org.jetbrains.compose.ui:ui" to "androidx.compose.ui:ui:1.10.6",
        "org.jetbrains.compose.ui:ui-android" to "androidx.compose.ui:ui-android:1.10.6",
        "org.jetbrains.compose.ui:ui-geometry" to "androidx.compose.ui:ui-geometry:1.10.6",
        "org.jetbrains.compose.ui:ui-geometry-android" to "androidx.compose.ui:ui-geometry-android:1.10.6",
        "org.jetbrains.compose.ui:ui-graphics" to "androidx.compose.ui:ui-graphics:1.10.6",
        "org.jetbrains.compose.ui:ui-graphics-android" to "androidx.compose.ui:ui-graphics-android:1.10.6",
        "org.jetbrains.compose.ui:ui-text" to "androidx.compose.ui:ui-text:1.10.6",
        "org.jetbrains.compose.ui:ui-text-android" to "androidx.compose.ui:ui-text-android:1.10.6",
        "org.jetbrains.compose.ui:ui-unit" to "androidx.compose.ui:ui-unit:1.10.6",
        "org.jetbrains.compose.ui:ui-unit-android" to "androidx.compose.ui:ui-unit-android:1.10.6",
        "org.jetbrains.compose.ui:ui-util" to "androidx.compose.ui:ui-util:1.10.6",
        "org.jetbrains.compose.ui:ui-util-android" to "androidx.compose.ui:ui-util-android:1.10.6",
        "org.jetbrains.compose.ui:ui-tooling" to "androidx.compose.ui:ui-tooling:1.10.6",
        "org.jetbrains.compose.ui:ui-tooling-android" to "androidx.compose.ui:ui-tooling-android:1.10.6",
        "org.jetbrains.compose.ui:ui-tooling-data" to "androidx.compose.ui:ui-tooling-data:1.10.6",
        "org.jetbrains.compose.ui:ui-tooling-data-android" to "androidx.compose.ui:ui-tooling-data-android:1.10.6",
        "org.jetbrains.compose.ui:ui-tooling-preview" to "androidx.compose.ui:ui-tooling-preview:1.10.6",
        "org.jetbrains.compose.ui:ui-tooling-preview-android" to "androidx.compose.ui:ui-tooling-preview-android:1.10.6",
        // foundation
        "org.jetbrains.compose.foundation:foundation" to "androidx.compose.foundation:foundation:1.10.6",
        "org.jetbrains.compose.foundation:foundation-android" to "androidx.compose.foundation:foundation-android:1.10.6",
        "org.jetbrains.compose.foundation:foundation-layout" to "androidx.compose.foundation:foundation-layout:1.10.6",
        "org.jetbrains.compose.foundation:foundation-layout-android" to "androidx.compose.foundation:foundation-layout-android:1.10.6",
        // animation
        "org.jetbrains.compose.animation:animation" to "androidx.compose.animation:animation:1.10.6",
        "org.jetbrains.compose.animation:animation-android" to "androidx.compose.animation:animation-android:1.10.6",
        "org.jetbrains.compose.animation:animation-core" to "androidx.compose.animation:animation-core:1.10.6",
        "org.jetbrains.compose.animation:animation-core-android" to "androidx.compose.animation:animation-core-android:1.10.6",
        // material3
        "org.jetbrains.compose.material3:material3" to "androidx.compose.material3:material3:1.4.0",
        "org.jetbrains.compose.material3:material3-android" to "androidx.compose.material3:material3-android:1.4.0",
        // material (icons)
        "org.jetbrains.compose.material:material-icons-core" to "androidx.compose.material:material-icons-core:1.7.8",
        "org.jetbrains.compose.material:material-icons-core-android" to "androidx.compose.material:material-icons-core-android:1.7.8",
        "org.jetbrains.compose.material:material-icons-extended" to "androidx.compose.material:material-icons-extended:1.7.8",
        "org.jetbrains.compose.material:material-icons-extended-android" to "androidx.compose.material:material-icons-extended-android:1.7.8",
      )
      for ((from, to) in rules) {
        substitute(module(from)).using(module(to)).because(
          "CMP android shims delegate to androidx 1.12.1 which requires AGP 9.1.0; " +
            "project is pinned to AGP 9.0.1 and uses BOM 2026.03.01 androidx versions"
        )
      }
    }
  }
}
