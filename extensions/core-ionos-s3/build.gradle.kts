plugins {
	`java-library`
	`maven-publish`
}

val edcGroup: String by project
val edcVersion: String by project
val metaModelVersion: String by project
val minIOVersion: String by project
val commonsCollectionsVersion: String by project
val extensionsGroup: String by project
val extensionsVersion: String by project

val gitHubPkgsName: String by project
val gitHubPkgsUrl: String by project
val gitHubUser: String? by project
val gitHubToken: String? by project

dependencies {
	api("${edcGroup}:runtime-metamodel:${metaModelVersion}")

	implementation("${edcGroup}:transfer-spi:${edcVersion}")
	implementation("${edcGroup}:validator-spi:${edcVersion}")

	implementation("io.minio:minio:${minIOVersion}")
	implementation("org.apache.commons:commons-collections4:${commonsCollectionsVersion}")
}

java {
	withJavadocJar()
	withSourcesJar()
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = extensionsGroup
			artifactId = "core-ionos-s3"
			version = extensionsVersion

			from(components["java"])

			pom {
				name.set("core-ionos-s3")
				description.set("Extension to manage an IONOS Cloud S3 storage")
			}
		}
	}
	repositories {
		maven {
			name = "GitHubPackages"
			url = uri("https://maven.pkg.github.com/${project.properties["github_owner"]}/${project.properties["github_repo"]}")
                    
			credentials {
				username = gitHubUser
				password = gitHubToken
			}
		}
	}
}