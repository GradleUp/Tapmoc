import cast.cast
import com.nfeld.jsonpathkt.kotlinx.resolvePathOrNull
import gratatouille.tasks.GInputFiles
import gratatouille.tasks.GTask
import gratatouille.GExtension
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.metadata.jvm.KotlinModuleMetadata
import kotlin.metadata.jvm.UnstableMetadataApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.Project
import org.gradle.api.component.Component
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Delete
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

@GExtension(pluginId = "check.publication", extensionName = "checkPublication")
abstract class CheckPublicationExtension(project: Project) {
  abstract val jvmTarget: Property<Int>
  abstract val kotlinMetadataVersion: Property<String>

  init {
    project.pluginManager.withPlugin("com.gradleup.tapmoc") {
      project.pluginManager.apply("maven-publish")

      with(project) {
        group = "com.example"
        extensions.getByType(PublishingExtension::class.java).apply {
          repositories {
            it.maven {
              it.name = "test"
              it.url = uri(file("build/m2"))
            }
          }
          publications { publications ->
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
              publications.create("maven", MavenPublication::class.java) { publication ->
                publication.from(components.getByName("java"))
              }
            }
            pluginManager.withPlugin("com.android.library") {
              /**
               * Doc says to use afterEvaluate 🤷‍♂️
               * https://developer.android.com/build/publish-library/upload-library
               */
              afterEvaluate {
                publications.create("maven", MavenPublication::class.java) { publication ->
                  publication.from(components.findByName("release"))
                }
              }
            }
          }
        }

        tasks.register("cleanM2", Delete::class.java) {
          delete(file("build/m2"))
        }
        tasks.named("publishAllPublicationsToTestRepository") {
          it.dependsOn("cleanM2")
        }

        val checkPublication = project.registerCheckPublicationTask(
          "checkPublication",
          m2Files = project.files(layout.buildDirectory.dir("m2")).asFileTree,
          jvmTarget = jvmTarget,
          kotlinMetadataVersion = kotlinMetadataVersion,
        )
        checkPublication.configure {
          it.dependsOn("publishAllPublicationsToTestRepository")
        }

        project.plugins.withType(LifecycleBasePlugin::class.java) {
          project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
            it.dependsOn(checkPublication)
          }
        }
      }
    }
  }
}


@OptIn(UnstableMetadataApi::class)
@GTask
fun checkPublication(m2Files: GInputFiles, jvmTarget: Int, kotlinMetadataVersion: String?) {
  m2Files.groupBy {
    /**
     * com/example/agp9-kmp
     */
    it.normalizedPath.split("/").take(3)
  }.values.forEach {
    checkPublicationInternal(
      publicationFiles = it.map { it.file },
      jvmTarget = jvmTarget,
      kotlinMetadataVersion = kotlinMetadataVersion,
    )
  }
}

@OptIn(UnstableMetadataApi::class)
private fun checkJarFile(name: String, inputStream: InputStream, jvmTarget: Int, kotlinMetadataVersion: String?) {
  //println("checkJarFile '$name'")
  ZipInputStream(inputStream).let { zis ->
    var entry = zis.nextEntry
    while (entry != null) {
      if (entry.name.endsWith(".class")) {

        ClassReader(zis).accept(
          object : ClassVisitor(Opcodes.ASM9) {
            override fun visit(
              version: Int,
              access: Int,
              name: String?,
              signature: String?,
              superName: String?,
              interfaces: Array<out String?>?,
            ) {
              check(version == jvmTarget + 44) {
                "${entry!!.name}: expected class files be of version '${jvmTarget + 44}', got '$version'"
              }
            }
          },
          0,
        )
      } else if (kotlinMetadataVersion != null && entry.name.endsWith(".kotlin_module")) {
        val metadata = KotlinModuleMetadata.read(zis.readAllBytes())
        metadata.version.apply {
          check(
            "$major.$minor.$patch" == kotlinMetadataVersion
              || kotlinMetadataVersion == "2.0.0" && "$major.$minor.$patch" == "1.9.9999", // kotlinc uses 1.9.9999 for 2.0.0. There is a good reason which I cannot remember today.
          ) {
            "${entry.name}: expected Kotlin metadata version '$kotlinMetadataVersion', got '$major.$minor.$patch'."
          }
        }
      }
      entry = zis.nextEntry
    }
  }
}


private fun checkPublicationInternal(publicationFiles: List<File>, jvmTarget: Int, kotlinMetadataVersion: String?) {
  val moduleFiles = publicationFiles.filter { it.extension == "module" }.toList()

  check(moduleFiles.size == 1) {
    "Expected exactly one module file, got ${moduleFiles.size}: $moduleFiles"
  }

  moduleFiles.single().readText().let {
    Json.parseToJsonElement(it)
  }.resolvePathOrNull("$.variants.*.attributes[\"org.gradle.jvm.version\"]")!!
    .cast<JsonArray>()
    .map {
      it.jsonPrimitive.content
    }
    .forEach {
      check(it == jvmTarget.toString()) {
        "Expected all modules to be built with Java '$jvmTarget', got $it"
      }
    }

  var found = false
  var jarFiles =
    publicationFiles.filter { (it.extension == "jar" && !it.name.endsWith("-sources.jar")) }
      .toList()
  if (jarFiles.size > 1) {
    "Too many jar files found, expected one, got ${jarFiles.size}: $jarFiles"
  } else if (jarFiles.size == 1) {
    found = true
    jarFiles.single().inputStream().use {
      checkJarFile(jarFiles.single().path, it, jvmTarget, kotlinMetadataVersion)
    }
  }

  if (found) {
    return
  }

  jarFiles = publicationFiles.filter { it.extension == "aar" }.toList()
  if (jarFiles.size > 1) {
    "Too many aar files found, expected one, got ${jarFiles.size}: $jarFiles"
  } else if (jarFiles.size == 1) {
    ZipInputStream(jarFiles.single().inputStream()).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        if (entry.name == "classes.jar") {
          if (found) {
            error("multiple classes.jar found in ${jarFiles.single()}")
          }
          found = true
          checkJarFile("${jarFiles.single().path}:${entry.name}", zis, jvmTarget, kotlinMetadataVersion)
        }
        entry = zis.nextEntry
      }
    }
  }

  if (found) {
    return
  }
}
