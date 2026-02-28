plugins {
    alias(libs.plugins.openapi.generator)
    `java-test-fixtures`
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/src/main/resources/spillkar-openapi.yml")
    outputDir.set(
        layout.buildDirectory
            .dir("generated/openapi")
            .get()
            .asFile.path,
    )
    modelPackage.set("no.nav.helse.spesialist.client.spillkar.generated")
    skipValidateSpec.set(true)
    globalProperties.set(
        mapOf(
            "models" to "",
            "apis" to "false",
            "supportingFiles" to "false",
            "modelTests" to "false",
            "modelDocs" to "false",
        ),
    )
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "serializationLibrary" to "jackson",
            "enumPropertyNaming" to "UPPERCASE",
        ),
    )
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin"))
        }
    }
}

tasks.openApiGenerate {
    doFirst { delete(outputDir) }
}

tasks.compileKotlin {
    dependsOn(tasks.openApiGenerate)
}

tasks.named("runKtlintCheckOverMainSourceSet") {
    dependsOn(tasks.openApiGenerate)
}

tasks.named("runKtlintFormatOverMainSourceSet") {
    dependsOn(tasks.openApiGenerate)
}

dependencies {
    api(project(":application"))

    implementation(libs.apache.httpclient5.fluent)
    implementation(libs.bundles.jackson)
    implementation(libs.micrometer.prometheus)

    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.wiremock)

    testFixturesImplementation(libs.wiremock)
}
