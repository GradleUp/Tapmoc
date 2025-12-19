package tapmoc.internal

internal fun kotlinVersionForGradle(major: Int, minor: Int): String {
  // See https://docs.gradle.org/current/userguide/compatibility.html#kotlin
  return when {
    major >= 9 && minor >= 2 -> "2.2.0"
    major >= 9 && minor >= 0 -> "2.2.0"
    major >= 8 && minor >= 12 -> "2.0.21"
    major >= 8 && minor >= 11 -> "2.0.20"
    major >= 8 && minor >= 10 -> "1.9.24"
    major >= 8 && minor >= 9 -> "1.9.23"
    major >= 8 && minor >= 7 -> "1.9.22"
    major >= 8 && minor >= 5 -> "1.9.20"
    major >= 8 && minor >= 4 -> "1.9.10"
    major >= 8 && minor >= 3 -> "1.9.0"
    major >= 8 && minor >= 2 -> "1.8.20"
    major >= 8 && minor >= 0 -> "1.8.10"
    major >= 7 && minor >= 6 -> "1.7.10"
    major >= 7 && minor >= 5 -> "1.6.21"
    major >= 7 && minor >= 3 -> "1.5.31"
    major >= 7 && minor >= 2 -> "1.5.21"
    major >= 7 && minor >= 0 -> "1.4.31"
    else -> error("Gradle versions < 7.0 are not supported (found '$major.$minor')")
  }
}

internal fun javaVersionForGradle(major: Int): Int {
  /**
   * See https://docs.gradle.org/current/userguide/compatibility.html#java
   */
  return when {
    major >= 9 -> 17
    major >= 5 -> 8
    else -> error("Gradle versions < 5 are not supported (found '$major')")
  }
}

internal fun parseGradleVersion(gradleVersion: String): MajorAnMinor {
  val c = gradleVersion.split('.')
  check (c.size >= 2) {
    "Tapmoc: gradleVersion should be in the form `major.minor[.patch]` (found '$gradleVersion')"
  }

  val major = c.get(0).toIntOrNull()
  val minor = c.get(1).toIntOrNull()

  check (major != null) {
    "Tapmoc: major version must be an integer (found '$major' in '$gradleVersion')"
  }
  check (minor != null) {
    "Tapmoc: minor version must be an integer (found '$minor' in '$gradleVersion')"
  }

  return MajorAnMinor(major, minor)
}
internal class MajorAnMinor(val major: Int, val minor: Int)
