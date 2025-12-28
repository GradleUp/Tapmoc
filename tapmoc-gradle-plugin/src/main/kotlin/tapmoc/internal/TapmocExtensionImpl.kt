package tapmoc.internal

import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Property
import org.gradle.language.base.plugins.LifecycleBasePlugin
import tapmoc.Severity
import tapmoc.TapmocExtension
import tapmoc.configureJavaCompatibility
import tapmoc.configureKotlinCompatibility
import tapmoc.task.registerTapmocCheckClassFileVersionsTask
import tapmoc.task.registerTapmocCheckKotlinMetadataVersionsTask
import tapmoc.task.registerTapmocCheckKotlinStdlibVersionsTask

internal abstract class TapmocExtensionImpl(private val project: Project) : TapmocExtension {
  abstract val javaClassFilesSeverity: Property<Severity>
  abstract val kotlinMetadataSeverity: Property<Severity>
  abstract val kotlinStdlibSeverity: Property<Severity>

  abstract val kotlinVersionProvider: Property<String>
  abstract val javaVersionProvider: Property<Int>

  init {
    javaClassFilesSeverity.convention(Severity.WARNING)
    kotlinMetadataSeverity.convention(Severity.WARNING)
    kotlinStdlibSeverity.convention(Severity.IGNORE)

    val apiDependencies = project.configurations.register("tapmocApiDependencies") {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true
      it.isVisible = false

      it.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_API))
    }

    val runtimeDependencies = project.configurations.register("tapmocRuntimeDependencies") {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true
      it.isVisible = false

      it.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
    }

    apiDependencies.configure { configuration ->
      project.getConfigurations("java-api").forEach {
        println("tapmocApiDependencies extends from ${it.name}")
        configuration.extendsFrom(it)
      }
    }

    runtimeDependencies.configure { configuration ->
      project.getConfigurations("java-runtime").forEach {
        println("tapmocRuntimeDependencies extends from ${it.name}")
        configuration.extendsFrom(it)
      }
    }

    val checkJavaClassFiles = project.registerTapmocCheckClassFileVersionsTask(
      warningAsError = javaClassFilesSeverity.map { it == Severity.ERROR },
      javaVersion = javaVersionProvider,
      jarFiles = project.files(apiDependencies, runtimeDependencies)
    )
    checkJavaClassFiles.configure {
      it.enabled = javaClassFilesSeverity.get() != Severity.IGNORE
    }

    val checkKotlinMetadatas = project.registerTapmocCheckKotlinMetadataVersionsTask(
      warningAsError = kotlinMetadataSeverity.map { it == Severity.ERROR },
      kotlinVersion = kotlinVersionProvider,
      files = project.files(apiDependencies),
    )
    checkKotlinMetadatas.configure {
      it.enabled = kotlinMetadataSeverity.get() != Severity.IGNORE
    }

    val checkKotlinStdlibs = project.registerTapmocCheckKotlinStdlibVersionsTask(
      warningAsError = kotlinStdlibSeverity.map { it == Severity.ERROR },
      kotlinVersion = kotlinVersionProvider,
      kotlinStdlibVersions = runtimeDependencies.map {
        it.incoming.resolutionResult.allComponents
          .mapNotNull { (it.id as? ModuleComponentIdentifier) }
          .filter {
            it.group == "org.jetbrains.kotlin" && it.module == "kotlin-stdlib"
          }.map {
            it.version
          }.toSet()
      },
    )
    checkKotlinStdlibs.configure {
      it.enabled = kotlinStdlibSeverity.get() != Severity.IGNORE
    }

    project.plugins.withType(LifecycleBasePlugin::class.java) {
      project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
        it.dependsOn(checkKotlinStdlibs)
        it.dependsOn(checkKotlinMetadatas)
        it.dependsOn(checkJavaClassFiles)
      }
    }
  }

  override fun java(version: Int) {
    javaVersionProvider.set(version)
    project.configureJavaCompatibility(version)
  }

  override fun kotlin(version: String) {
    kotlinVersionProvider.set(version)
    project.configureKotlinCompatibility(version)
  }

  override fun gradle(gradleVersion: String) {
    val major = parseGradleMajorVersion(gradleVersion)
    kotlin(kotlinVersionForGradle(major))
    java(javaVersionForGradle(major))
  }

  override fun javaVersionForGradle(gradleVersion: String): Int {
    return javaVersionForGradle(parseGradleMajorVersion(gradleVersion))
  }

  override fun kotlinVersionForGradle(gradleVersion: String): String {
    return kotlinVersionForGradle(parseGradleMajorVersion(gradleVersion))
  }

  override fun checkJavaClassFileVersion(severity: Severity) {
    javaClassFilesSeverity.set(severity)
  }

  override fun checkKotlinMetadata(severity: Severity) {
    kotlinMetadataSeverity.set(severity)
  }

  override fun checkKotlinStdlibDependencies(severity: Severity) {
    kotlinStdlibSeverity.set(severity)
  }

  override fun checkDependencies() {
    checkDependencies(Severity.ERROR)
  }

  @Suppress("DEPRECATION")
  override fun checkDependencies(severity: Severity) {
    checkJavaClassFileVersion(severity)
    checkKotlinMetadata(severity)
  }

  @Deprecated("Use checkDependencies instead.", replaceWith = ReplaceWith("checkDependencies(severity)"), level = DeprecationLevel.ERROR)
  override fun checkApiDependencies(severity: Severity) {
    TODO()
  }

  @Deprecated("Use checkDependencies instead.", replaceWith = ReplaceWith("checkDependencies(severity)"), level = DeprecationLevel.ERROR)
  override fun checkRuntimeDependencies(severity: Severity) {
    TODO()
  }
}

/**
 * Retrieves the outgoing configurations for this project.
 *
 * We currently only check the JVM configurations.
 */
private fun Project.getConfigurations(usage: String): NamedDomainObjectSet<Configuration> {
  return configurations.matching {
    it.isCanBeConsumed
      && it.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name == usage
  }
}

