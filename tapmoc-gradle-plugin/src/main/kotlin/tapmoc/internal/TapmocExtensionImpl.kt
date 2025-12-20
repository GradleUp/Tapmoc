package tapmoc.internal

import org.gradle.api.NamedDomainObjectProvider
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
  private var kotlinMetadataSeverity = Severity.ERROR
  private var kotlinStdlibSeverity = Severity.ERROR

  private val apiDependencies: NamedDomainObjectProvider<Configuration>
  private val runtimeDependencies: NamedDomainObjectProvider<Configuration>

  abstract val kotlinVersionProvider: Property<String>
  abstract val javaVersionProvider: Property<Int>

  init {
    apiDependencies = project.configurations.register("tapmocApiDependencies") {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true
      it.isVisible = false

      it.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_API))
    }

    runtimeDependencies = project.configurations.register("tapmocRuntimeDependencies") {
      it.isCanBeConsumed = false
      it.isCanBeResolved = true
      it.isVisible = false

      it.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
    }

    val checkKotlinMetadatas = project.registerTapmocCheckKotlinMetadataVersionsTask(
      warningAsError = project.provider { kotlinMetadataSeverity == Severity.ERROR },
      kotlinVersion = kotlinVersionProvider,
      files = project.files(apiDependencies),
    )

    val checkKotlinStdlibs = project.registerTapmocCheckKotlinStdlibVersionsTask(
      warningAsError = project.provider { kotlinStdlibSeverity == Severity.ERROR },
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

    val checkJavaClassFiles = project.registerTapmocCheckClassFileVersionsTask(
      warningAsError = project.provider { kotlinStdlibSeverity == Severity.ERROR },
      javaVersion = javaVersionProvider,
      jarFiles = project.files(apiDependencies, runtimeDependencies)
    )

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

  override fun checkDependencies() {
    checkDependencies(Severity.ERROR)
  }

  @Suppress("DEPRECATION")
  override fun checkDependencies(severity: Severity) {
    checkApiDependencies(severity)
    checkRuntimeDependencies(severity)
  }

  @Deprecated("Use checkDependencies instead.", replaceWith = ReplaceWith("checkDependencies(severity)"))
  override fun checkApiDependencies(severity: Severity) {
    if (severity == Severity.IGNORE) {
      return
    }
    kotlinMetadataSeverity = severity

    apiDependencies.configure {
      it.dependencies.add(project.dependencies.project(mapOf("path" to project.path)))
    }
  }

  @Deprecated("Use checkDependencies instead.", replaceWith = ReplaceWith("checkDependencies(severity)"))
  override fun checkRuntimeDependencies(severity: Severity) {
    if (severity == Severity.IGNORE) {
      return
    }
    kotlinStdlibSeverity = severity

    runtimeDependencies.configure {
      it.dependencies.add(project.dependencies.project(mapOf("path" to project.path)))
    }
  }
}



