package tapmoc

interface TapmocExtension {
  /**
   * Configures the version of Java to target.
   * This version is used as:
   * - targetCompatibility
   * - sourceCompatibility
   * - release (if not on android)
   *
   * @param version the version of Java to target.
   * Examples: 8, 11, 17, 21, 24, ...
   */
  fun java(version: Int)

  /**
   * Configures the version of Kotlin to target.
   * This version is used as:
   * - languageVersion
   * - apiVersion
   * - coreLibrariesVersion
   *
   *
   * @param version the version of Kotlin to target.
   * This is a string in case you need a specific minor version in `coreLibrariesVersion`
   *
   * Examples: "1.9.0", "1.9.22", "2.0.21", "2.1.20", ...
   */
  fun kotlin(version: String)

  /**
   * Configures the minimal Gradle version supported.
   *
   * This method:
   * - Calls [kotlin] with the compatible Kotlin version as described in the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html#kotlin).
   * - Calls [java] with the compatible Java version as described in the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html#java_runtime).
   *
   * @param gradleVersion the Gradle version to target, specified as a string. Example: "8.14".
   */
  fun gradle(gradleVersion: String)

  /**
   * Returns the minimal version of Java required to run the given Gradle version.
   *
   * Gradle versions between 2.0 and 8.14 require Java 8 (but you may run Gradle 8.14 with Java 24 if you want to).
   * Gradle 9.0.0 requires Java 17.
   *
   * See https://docs.gradle.org/current/userguide/compatibility.html#java
   */
  fun javaVersionForGradle(gradleVersion: String): Int

  /**
   * Returns the version of Kotlin embedded in the Gradle distribution.
   *
   * See https://docs.gradle.org/current/userguide/compatibility.html#kotlin
   */
  fun kotlinVersionForGradle(gradleVersion: String): String

  @Deprecated("Use checkDependencies instead.", ReplaceWith("checkDependencies(severity)"))
  fun checkApiDependencies(severity: Severity)

  @Deprecated("Use checkDependencies instead.", ReplaceWith("checkDependencies(severity)"))
  fun checkRuntimeDependencies(severity: Severity)

  /**
   * Walks the consumable configurations exposing a `java-api` or `java-runtime` [usage attribute](https://docs.gradle.org/9.2.1/javadoc/org/gradle/api/attributes/Usage.html)
   * and checks that dependencies are compatible with the target [java] and [kotlin] values:
   *
   * - checks that `kotlin-stdlib` is always <= targetKotlinVersion (`java-runtime` only)
   * - checks that Kotlin metadata is always <= targetKotlinVersion + 1 (`java-api` only).
   * Note: it is `targetKotlinVersion + 1` because Kotlin has a [best effort n + 1 forward compatibility guarantee](https://kotlinlang.org/docs/kotlin-evolution-principles.html#evolving-the-binary-format).
   * - checks that the Java class files version is always <= targetJavaVersion
   */
  fun checkDependencies(severity: Severity)

  /**
   * Walks the consumable configurations exposing a `java-api` or `java-runtime` [usage attribute](https://docs.gradle.org/9.2.1/javadoc/org/gradle/api/attributes/Usage.html)
   * and checks that dependencies are compatible with the target [java] and [kotlin] values:
   *
   * - checks that `kotlin-stdlib` is always <= targetKotlinVersion (`java-runtime` only)
   * - checks that Kotlin metadata is always <= targetKotlinVersion + 1 (`java-api` only).
   * Note: it is `targetKotlinVersion + 1` because Kotlin has a [best effort n + 1 forward compatibility guarantee](https://kotlinlang.org/docs/kotlin-evolution-principles.html#evolving-the-binary-format).
   * - checks that the Java class files version is always <= targetJavaVersion
   */
  fun checkDependencies()
}

enum class Severity {
  IGNORE,
  WARNING,
  ERROR
}
