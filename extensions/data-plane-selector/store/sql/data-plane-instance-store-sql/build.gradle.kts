plugins {
    `java-library`
    `maven-publish`
}


dependencies {
    api(project(":spi:common:transaction-spi"))
    api(project(":spi:data-plane-selector:data-plane-selector-spi"))

    implementation(project(":spi:common:transaction-datasource-spi"))
    implementation(project(":extensions:common:sql:sql-core"))

    testImplementation(project(":core:common:junit"))
    testImplementation(testFixtures(project(":spi:data-plane-selector:data-plane-selector-spi")))
    testImplementation(testFixtures(project(":extensions:common:sql:sql-core")))


}

publishing {
    publications {
        create<MavenPublication>(project.name) {
            from(components["java"])
        }
    }
}
