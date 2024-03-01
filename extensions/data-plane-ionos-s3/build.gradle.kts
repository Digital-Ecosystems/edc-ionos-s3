plugins {
    `java-library`
    `maven-publish`
}

val javaVersion: String by project
val edcGroup: String by project
val edcVersion: String by project
val extensionsGroup: String by project
val extensionsVersion: String by project
val junitVersion: String by project
val mockitoVersion: String by project

val gitHubPkgsName: String by project
val gitHubPkgsUrl: String by project
val gitHubUser: String? by project
val gitHubToken: String? by project

dependencies {
	api("${edcGroup}:data-plane-spi:${edcVersion}")

	implementation(project(":extensions:core-ionos-s3"))
	implementation("${edcGroup}:util:${edcVersion}")
	implementation("${edcGroup}:transfer-spi:${edcVersion}")
	implementation("${edcGroup}:data-plane-util:${edcVersion}")
    implementation("${edcGroup}:data-plane-core:${edcVersion}")
	implementation("${edcGroup}:http:${edcVersion}")
	implementation("${edcGroup}:validator-spi:${edcVersion}")
			
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