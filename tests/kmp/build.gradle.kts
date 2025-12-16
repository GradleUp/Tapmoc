plugins {
  id("com.gradleup.tapmoc")
  id("org.jetbrains.kotlin.multiplatform").version("2.3.0")
  id("check.publication")
}

tapmoc {
  java(11)
  kotlin("2.0.0")
  checkDependencies()
}

kotlin {
  jvm()
  macosArm64()
  js {
    browser()
  }

  applyDefaultHierarchyTemplate()

  sourceSets {
    val jvmAndMacos by creating {
      dependsOn(commonMain.get())
    }

    macosArm64Main.get().dependsOn(jvmAndMacos)
    jvmMain.get().dependsOn(jvmAndMacos)
  }
}

checkPublication {
  jvmTarget.set(11)
  kotlinMetadataVersion.set("2.0.0")
}
