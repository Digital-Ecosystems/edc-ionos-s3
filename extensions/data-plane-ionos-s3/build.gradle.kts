plugins {
    `java-library`
    `maven-publish`
}

val edcGroup: String by project
val edcVersion: String by project
val metaModelVersion: String by project
val extensionsGroup: String by project
val extensionsVersion: String by project
val junitVersion: String by project
val mockitoVersion: String by project

val gitHubPkgsName: String by project
val gitHubPkgsUrl: String by project
val gitHubUser: String? by project
val gitHubToken: String? by project

dependencies {
	api("${edcGroup}:runtime-metamodel:${metaModelVersion}")

	implementation("${edcGroup}:util-lib:${edcVersion}")
	implementation("${edcGroup}:transfer-spi:${edcVersion}")
	implementation("${edcGroup}:validator-spi:${edcVersion}")
	implementation("${edcGroup}:data-plane-util:${edcVersion}")

	implementation(project(":extensions:core-ionos-s3"))

	testImplementation("${edcGroup}:junit:${edcVersion}")
	testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
	testImplementation("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
	testImplementation("org.mockito:mockito-core:${mockitoVersion}")
}

java {
	withJavadocJar()
	withSourcesJar()
}

tasks.test {
	useJUnitPlatform()
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = extensionsGroup
			artifactId = "data-plane-ionos-s3"
			version = extensionsVersion

			from(components["java"])

			pom {
				name.set("data-plane-ionos-s3")
				description.set("Extension to perform the data exchange process using an IONOS Cloud S3 storage")
			}
		}
	}
	repositories {
		maven {
			name = gitHubPkgsName
			url = uri("https://maven.pkg.github.com/${project.properties["github_owner"]}/${project.properties["github_repo"]}")
                
			credentials {
				username = gitHubUser
				password = gitHubToken
			}
		}
	}
}