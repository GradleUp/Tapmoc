plugins {
  id("com.gradleup.tapmoc")
  `embedded-kotlin`
  id("check.publication")
  id("java-gradle-plugin")
}

tapmoc {
  // Gradle 8.3 uses Kotlin languageVersion 1.8
  gradle("8.3")
  checkDependencies()
}

checkPublication {
  jvmTarget.set(8)
  kotlinMetadataVersion.set("1.8.0")
}

dependencies {
  compileOnlyApi("dev.gradleplugins:gradle-api:8.3")
}
