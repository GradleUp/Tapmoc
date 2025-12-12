plugins {
  id("com.gradleup.tapmoc")
  `embedded-kotlin`
  id("check.publication")
  id("java-gradle-plugin")
}

tapmoc {
  java(11)
  kotlin("1.9.0")
  checkDependencies()
}

checkPublication {
  jvmTarget.set(11)
  kotlinMetadataVersion.set("1.9.0")
}
