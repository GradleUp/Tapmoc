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
   * Configures compatibility flags for the minimal Gradle version supported and enables all dependencies checks.
   *
   * This method:
   * - Calls [kotlin] with the compatible Kotlin version as described in the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html#kotlin).
   * - Calls [java] with the compatible Java version as described in the [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html#java_runtime).
   * - Calls `checkDependencies(Severity.ERROR)`
   * - Calls `checkKotlinStdlibDependencies(Severity.ERROR)`
   *
   * It is equivalent to the following code:
   * ```kotlin
   * kotlin(kotlinVersionForGradle(gradleVersion))
   * java(javaVersionForGradle(gradleVersion))
   * checkDependencies(Severity.ERROR)
   * checkKotlinStdlibDependencies(Severity.ERROR)
   * ```
   *
   * @param gradleVersion the Gradle version to target, specified as a string. Example: "8.14".
   */
  fun gradle(gradleVersion: String)

  /**
   * Returns the minimal version of Java required to run the given Gradle version.
   *
   * Gradle versions between 2.0 and 8.14 require Java 8.
   * Gradle 9.0.0 requires Java 17.
   *
   * See https://docs.gradle.org/current/userguide/compatibility.html#java
   */
  fun javaVersionForGradle(gradleVersion: String): Int

  /**
   * Returns the languageVersion used to compile Kotlin build scripts.
   *
   * See https://docs.gradle.org/current/userguide/compatibility.html#kotlin
   */
  fun kotlinVersionForGradle(gradleVersion: String): String

  /**
   * Checks that the api and runtime dependencies are compatible with the target Java version.
   *
   * This checks the [class file version](https://docs.oracle.com/javase/specs/jvms/se25/html/jvms-4.html#jvms-4.1).
   *
   * @param severity The severity level for the check. Defaults to `Severity.WARNING`.
   */
  fun checkJavaClassFileVersion(severity: Severity)

  /**
   * Checks that the api dependencies Kotlin metadata is compatible with the target Kotlin version.
   *
   * Thanks to Kotlin [best effort n + 1 forward compatibility guarantee](https://kotlinlang.org/docs/kotlin-evolution-principles.html#evolving-the-binary-format),
   * dependencies may contain `kotlinTarget + 1` metadata.
   *
   * @param severity The severity level for the check. Defaults to `Severity.WARNING`.
   */
  fun checkKotlinMetadata(severity: Severity)

  /**
   * Checks that the runtime dependencies do not contain a version of `kotlin-stdlib` higher than the target Kotlin version.
   *
   * In most cases, `kotlin-stdlib` can be safely upgraded and this check is disabled by default.
   *
   * Enable it if your runtime forces a given version of `kotlin-stdlib`. This is the notably case for Gradle plugins.
   *
   * @param severity The severity level for the check. Defaults to `Severity.IGNORE`.
   */
  fun checkKotlinStdlibDependencies(severity: Severity)

  /**
   * This is equivalent to calling `checkJavaClassFileVersion(severity)` and `checkKotlinMetadata(severity)`.
   *
   * @see checkJavaClassFileVersion
   * @see checkKotlinMetadata
   */
  fun checkDependencies(severity: Severity)

  /**
   * This is equivalent to calling `checkDependencies(Severity.ERROR)`.
   *
   * @see checkDependencies
   */
  fun checkDependencies()

  @Deprecated("Use checkDependencies instead.", ReplaceWith("checkDependencies(severity)"), level = DeprecationLevel.ERROR)
  fun checkApiDependencies(severity: Severity)

  @Deprecated("Use checkDependencies instead.", ReplaceWith("checkDependencies(severity)"), level = DeprecationLevel.ERROR)
  fun checkRuntimeDependencies(severity: Severity)
}

enum class Severity {
  IGNORE,
  WARNING,
  ERROR
}
