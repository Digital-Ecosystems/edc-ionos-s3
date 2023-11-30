/*
 * This file was generated by the Gradle 'init' task.
 *
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/7.4.2/userguide/multi_project_builds.html
 */


dependencyResolutionManagement {
    repositories {
        
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from("org.eclipse.edc:edc-versions:0.1.2")
        }
    }
}

include(":edc-ionos-extension:data-plane-ionos-s3")

include(":edc-ionos-extension:provision-ionos-s3")

include(":edc-ionos-extension:core-ionos-s3")

include(":edc-ionos-extension:vault-hashicorp")

include(":connector")
include(":connector-persistence")

include(":example:file-transfer-push-daps:transfer-file")
include(":example:file-transfer-push-daps:provider")
include(":example:file-transfer-push-daps:consumer")
include(":example:file-transfer-push:provider")
include(":example:file-transfer-push:consumer")
include(":example:file-transfer-push:transfer-file")
include(":example:file-transfer-persistence:provider")
include(":example:file-transfer-persistence:consumer")
include(":example:file-transfer-persistence:transfer-file")
include(":example:file-transfer-pull:connector")
include(":example:file-transfer-pull:consumer")
include(":example:file-transfer-pull:provider")
include(":example:file-transfer-pull:backend-service")
include(":example:file-transfer-multicloud:provider")
include(":example:file-transfer-multicloud:consumer")
