plugins {
  `java-library`
  `maven-publish`
}

configure<PublishingExtension> {
  publications {
    withType(MavenPublication::class.java) {
      pom {
        url.set("https://github.com/ionos-cloud/edc-ionos-s3")
        licenses {
          license {
            name.set("Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0")
          }
        }
        developers {
          developer {
            id.set("jannotti-glaucio")
            name.set("Glaucio Jannotti")
            email.set("glaucio.jannotti@ionos.com")
          }
          developer {
            id.set("paulolory-ionos")
            name.set("Paulo Lory")
            email.set("paulo.lory@ionos.com")
          }
          developer {
            id.set("paulocabrita-ionos")
            name.set("Paulo Cabrita")
            email.set("paulo.cabrita@ionos.com")
          }
        }
        scm {
          connection.set("scm:git:git@github.com:ionos-cloud/edc-ionos-s3.git")
          developerConnection.set("scm:git:git@github.com:ionos-cloud/edc-ionos-s3.git")
          url.set("https://github.com/ionos-cloud/edc-ionos-s3")
        }
      }
    }
  }
}