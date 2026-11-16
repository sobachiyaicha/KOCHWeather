plugins {
  alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
  // iOS 16.3 is the deployment target; the minimum supported by Kotlin/Native
  // for these targets is 14.0, so the linker flag is what counts.
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
      // Compose Multiplatform will land here when its version for Kotlin 2.3.20
      // is pinned; the module is intentionally dependency-free for now so the
      // iOS pipeline stays green.
    }
    commonTest.dependencies {
      implementation(kotlin("test"))
    }
  }

  compilerOptions {
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }
}
