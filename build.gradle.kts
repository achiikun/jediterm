val projectVersion = rootProject.projectDir.resolve("VERSION").readText().trim() +
  if (System.getenv("INTELLIJ_DEPENDENCIES_BOT") == null) "-SNAPSHOT" else ""

plugins {
  `maven-publish`
}

allprojects {
  version = projectVersion
  group = "org.jetbrains.jediterm"
  layout.buildDirectory = rootProject.projectDir.resolve(".gradleBuild/" + project.name)
}

subprojects {
  repositories {
    mavenCentral()
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      groupId = "org.jetbrains.jediterm"
      artifactId = project.name
      version = projectVersion
    }
  }
}
