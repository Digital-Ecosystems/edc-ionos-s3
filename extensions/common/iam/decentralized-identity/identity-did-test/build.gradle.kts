plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {

    // newer Nimbus versions create a version conflict with the MSAL library which uses this version as a transitive dependency
    testFixturesApi(libs.nimbus.jwt)

}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
