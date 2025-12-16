plugins {
  id("com.gradleup.tapmoc")
  id("org.jetbrains.kotlin.multiplatform").version("2.3.0-RC3")
  id("check.publication")
}

tapmoc {
  java(11)
  kotlin("2.0.0")
  checkDependencies()
}

kotlin {
  jvm()
  iosArm64()
}

checkPublication {
  jvmTarget.set(11)
  kotlinMetadataVersion.set("2.0.0")
}
