plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

kotlin {
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
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
  }
}
