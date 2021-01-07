import io.insource.build.Publishing
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java")
  id("maven")
  id("signing")
  id("maven-publish")
  id("org.jetbrains.kotlin.jvm") version "1.3.72"
  id("org.jetbrains.kotlin.plugin.spring") version "1.3.72"
}

tasks.register<Jar>("sourcesJar") {
  from(sourceSets.main.get().allSource)
  archiveClassifier.set("sources")
}

tasks.register<Jar>("javadocJar") {
  from(tasks.javadoc)
  archiveClassifier.set("javadoc")
}

publishing {
  repositories {
    maven {
      url = uri("https://maven.pkg.github.com/InSourceSoftware/in-rules-engine")
      credentials {
        username = project.findProperty("servers.github.username")?.toString() ?: System.getenv("GITHUB_USERNAME")
        password = project.findProperty("servers.github.password")?.toString() ?: System.getenv("GITHUB_PASSWORD")
      }
    }
    maven {
      name = "ossrh"
      url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
      credentials {
        username = project.findProperty("servers.ossrh.username")?.toString() ?: System.getenv("OSSRH_USERNAME")
        password = project.findProperty("servers.ossrh.password")?.toString() ?: System.getenv("OSSRH_PASSWORD")
      }
    }
  }

  publications {
    create<MavenPublication>("maven") {
      groupId = Publishing.groupId
      version = Publishing.version

      from(components["kotlin"])
      artifact(tasks["sourcesJar"])
      artifact(tasks["javadocJar"])

      pom {
        name.set(Publishing.artifactId)
        description.set(Publishing.description)
        url.set(Publishing.url)
        licenses {
          license {
            name.set(Publishing.license)
            url.set(Publishing.licenseUrl)
          }
        }
        developers {
          developer {
            id.set(Publishing.developerUserName)
            name.set(Publishing.developerFullName)
            email.set(Publishing.developerEmailAddress)
          }
        }
        scm {
          connection.set(Publishing.connectionUrl)
          developerConnection.set(Publishing.developerConnectionUrl)
          url.set(Publishing.url)
        }
      }
    }
  }
}

signing {
  sign(publishing.publications["maven"])
}

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.11.1")
  testImplementation("junit:junit:4.12")
  testImplementation("org.hamcrest:hamcrest:2.1")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "1.8"
  }
}