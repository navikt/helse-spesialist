import com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLGenerateClientTask
import com.expediagroup.graphql.plugin.gradle.tasks.GraphQLIntrospectSchemaTask

plugins {
    kotlin("plugin.serialization") version "1.5.31"
    id("com.expediagroup.graphql") version "5.2.0"
}

dependencies {
    implementation(project(":spesialist-felles"))
    implementation(project(":spesialist-api"))
    testImplementation(project(":testkode"))
}

val graphqlIntrospectSchema by tasks.getting(GraphQLIntrospectSchemaTask::class) {
    endpoint.set("https://spleis-api.dev-fss-pub.nais.io/graphql")
    outputFile.set(File("${project.projectDir}/src/main/resources/graphql/schema.graphql"))
    onlyIf { false }
}

val graphqlGenerateClient by tasks.getting(GraphQLGenerateClientTask::class) {
    val baseDir = "${project.projectDir}/src/main/resources/graphql"

    serializer.set(GraphQLSerializer.KOTLINX)
    packageName.set("no.nav.helse.mediator.graphql")
    schemaFile.set(graphqlIntrospectSchema.outputFile)
    queryFiles.from(listOf(
        File("$baseDir/hentEldreGenerasjoner.graphql"),
        File("$baseDir/hentSnapshot.graphql"))
    )

    dependsOn("graphqlIntrospectSchema")
}

tasks {
    named<Jar>("jar") {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.AppKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}
