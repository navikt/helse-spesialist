package no.nav.helse.spesialist.api.rest.graphqlgenerator

import no.nav.helse.spesialist.api.rest.GetHåndterer
import no.nav.helse.spesialist.api.rest.PostHåndterer
import no.nav.helse.spesialist.api.rest.RESTHÅNDTERERE
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

class Generator {
    fun generate() {
        RESTHÅNDTERERE.forEach { håndterer ->
            println("Genererer output-typer nødvendige for ${håndterer::class.simpleName}...")
            generateOutputTypesRecursively(håndterer.responseBodyType)
        }
        RESTHÅNDTERERE.filterIsInstance<PostHåndterer<*, *, *>>().forEach { håndterer ->
            println("Genererer input-typer nødvendige for ${håndterer::class.simpleName}...")
            generateInputTypesRecursively(håndterer.requestBodyType)
        }
        RESTHÅNDTERERE.forEach { håndterer ->
            println("--- Genererer for ${håndterer::class.simpleName} ---")
            val operationName = håndterer::class.simpleName!!.removeSuffix("Håndterer")
            val urlParameters =
                håndterer.urlParametersClass.declaredMemberProperties
                    .associate { property -> property.name.replaceÆØÅ() to property.returnType.toUrlParameterTypeReference() }
            val responseBodyType = håndterer.responseBodyType

            val responseBodyTypeReference = responseBodyType.resolveTypeReference()

            when (håndterer) {
                is GetHåndterer<*, *> -> {
                    println("Genererer query siden dette er en GET-operasjon...")
                    val query = """
            query REST$operationName(${urlParameters.entries.joinToString(separator = ", ") { (name, type) -> $$"$$$name: $$type" }}) {
                rest$operationName(${urlParameters.keys.joinToString { $$"$$it: $$$it" }})
                @rest(
                    type: "$responseBodyTypeReference"
                    endpoint: "spesialist"
                    path: "/${håndterer.urlPath.gqlParameterizedPath()}"
                    method: "GET"
                )""".trimIndent() + printQueryTree(responseBodyType) + "\n}"
                    generatedQueries.add(
                        "rest$operationName(${
                            urlParameters.entries.joinToString(separator = ", ") { (name, type) -> "$name: $type" }
                        }): $responseBodyTypeReference"
                    )
                    val outputPath = "../helse-speil/src/io/graphql/rest/spesialist/$operationName.query.graphql"
                    println("Lagrer query som $outputPath...")
                    File(outputPath).writeText(query)
                }

                is PostHåndterer<*, *, *> -> {
                    val requestBodyType = håndterer.requestBodyType
                    val requestBodyTypeReference = requestBodyType.resolveTypeReference()
                    println("Genererer mutation siden dette er en POST-operasjon...")
                    val parameters = urlParameters + ("input" to requestBodyTypeReference)
                    val query = $$"""
            mutation REST$$operationName($${parameters.entries.joinToString(separator = ", ") { (name, type) -> $$"$$$name: $$type" }}) {
                rest$$operationName($${parameters.keys.joinToString { $$"$$it: $$$it" }})
                @rest(
                    type: "$$responseBodyTypeReference"
                    endpoint: "spesialist"
                    path: "/$${håndterer.urlPath.gqlParameterizedPath()}"
                    method: "POST"
                )""".trimIndent() + printQueryTree(responseBodyType) + "\n}"
                    generatedMutations.add(
                        "rest$operationName(${
                            parameters.entries.joinToString(separator = ", ") { (name, type) -> "$name: $type" }
                        }): $responseBodyTypeReference"
                    )
                    val outputPath = "../helse-speil/src/io/graphql/rest/spesialist/$operationName.mutation.graphql"
                    println("Lagrer mutation som $outputPath...")
                    File(outputPath).writeText(query)
                }
            }
        }
    }

    private fun printQueryTree(responseBodyType: KType): String =
        responseBodyType.resolveTypeReference().type.printQueryPropertyTree(2)

    private fun KType.resolveTypeReference(): TypeReference =
        (scalarTypes[withNullability(false)]
            ?: outputTypes[withNullability(false)]
            ?: inputTypes[withNullability(false)]
            ?: error("På en eller annen måte har ikke $this blitt generert"))
            .toReference(isMarkedNullable)

    val inputTypes = mutableMapOf<KType, InputTypeDefinition>()
    val outputTypes = mutableMapOf<KType, OutputTypeDefinition>()
    val generatedQueries = mutableListOf<String>()
    val generatedMutations = mutableListOf<String>()

    private fun generateOutputTypesRecursively(type: KType): TypeReference {
        val key = type.withNullability(false)
        return (when {
            scalarTypes.containsKey(key) -> scalarTypes[key]!!
            outputTypes.containsKey(key) -> outputTypes[key]!!

            type.isCollection() -> OutputCollectionTypeDefinition(
                elementType = generateOutputTypesRecursively(type.getCollectionElementType())
            ).also { outputTypes[key] = it }

            else -> {
                println("Genererer output-type for $type...")
                val klass = type.classifier as KClass<*>
                val supertypes = klass.supertypes.filterNot { it == typeOf<Any>() }
                OutputObjectTypeDefinition(
                    name = klass.gqlTypeName(),
                    implements = supertypes
                        .map { generateOutputTypesRecursively(type = it).type },
                    isInterface = klass.java.isInterface,
                    properties = klass.declaredMemberProperties
                        .filterNot { it.name == "__typename" }
                        .onEach { it.name.assertNoÆØÅ() }
                        .associate { it.name to generateOutputTypesRecursively(type = it.returnType) }
                )
                    .also { outputTypes[key] = it }
                    .also {
                        // Pass på å generere alle undertyper, selv om de ikke er eksplisitt referert til
                        klass.sealedSubclasses.forEach {
                            generateOutputTypesRecursively(type = it.starProjectedType)
                        }
                    }
            }
        }).toReference(type.isMarkedNullable)
    }

    private fun generateInputTypesRecursively(type: KType): TypeReference {
        val key = type.withNullability(false)
        return (when {
            scalarTypes.containsKey(key) -> scalarTypes[key]!!
            inputTypes.containsKey(key) -> inputTypes[key]!!

            type.isCollection() -> InputCollectionTypeDefinition(
                elementType = generateInputTypesRecursively(type.getCollectionElementType())
            ).also { inputTypes[key] = it }

            else -> {
                println("Genererer input-type for $type...")
                val klass = type.classifier as KClass<*>

                klass.sealedSubclasses
                    .takeUnless { it.isEmpty() }
                    ?.let { error("$klass har (sealed) subclasses (${it.joinToString()}), dette støttes ikke i input") }

                klass.supertypes.filterNot { it == typeOf<Any>() }
                    .takeUnless { it.isEmpty() }
                    ?.let { error("$klass extender en annen type (${it.joinToString()}), dette støttes ikke i input") }

                InputObjectTypeDefinition(
                    name = klass.gqlTypeName() + if (outputTypes.containsKey(key)) "Input" else "",
                    properties = klass.declaredMemberProperties
                        .filterNot { it.name == "__typename" }
                        .onEach { it.name.assertNoÆØÅ() }
                        .associate { it.name to generateInputTypesRecursively(type = it.returnType) }
                ).also { inputTypes[key] = it }
            }
        }).toReference(type.isMarkedNullable)
    }

    private fun KType.isCollection(): Boolean = (classifier as? KClass<*>) in setOf(
        List::class,
        Set::class
    )

    private fun KType.getCollectionElementType(): KType = this.arguments.single().type!!

    private fun TypeDefinition.toReference(nullable: Boolean): TypeReference =
        TypeReference(type = this, nullable = nullable)

    private fun KType.toUrlParameterTypeReference(): TypeReference =
        TypeReference(
            type = scalarTypes[withNullability(false)]
                ?: error("Type $this er ikke en skalar type. Kun skalare typer kan brukes som URL-parametre."),
            nullable = isMarkedNullable
        )

    private fun TypeDefinition.printQueryPropertyTree(indentLevel: Int): String {
        return when (this) {
            is InputTypeDefinition -> {
                error("Gir ingen mening å spørre etter felter i en inputtype")
            }

            is OutputCollectionTypeDefinition -> {
                elementType.type.printQueryPropertyTree(indentLevel)
            }

            is ScalarTypeDefinition -> {
                ""
            }

            is OutputObjectTypeDefinition -> {
                buildString {
                    append(" {")
                    append("\n")
                    properties.entries.sortedBy { it.key }
                        .filterNot { property ->
                            this@printQueryPropertyTree.implements.map { it as OutputObjectTypeDefinition }
                                .any { it.properties.containsKey(property.key) }
                        }
                        .forEach { (key, typeReference) ->
                            append((1..indentLevel).joinToString(separator = "") { "    " })
                            append(key)
                            append(typeReference.type.printQueryPropertyTree(indentLevel + 1))
                            append("\n")
                        }
                    outputTypes.values.filterIsInstance<OutputObjectTypeDefinition>()
                        .filter {
                            it.properties
                                .filterNot { property ->
                                    it.implements.map { it as OutputObjectTypeDefinition }
                                        .any { it.properties.containsKey(property.key) }
                                }
                                .isNotEmpty()
                        }
                        .filter { this@printQueryPropertyTree in it.implements }
                        .sortedBy { it.name }
                        .forEach { subtype ->
                            append((1..indentLevel).joinToString(separator = "") { "    " })
                            append("... on ${subtype.name}" + subtype.printQueryPropertyTree(indentLevel + 1))
                            append("\n")
                        }
                    append((1..<indentLevel).joinToString(separator = "") { "    " })
                    append("}")
                }
            }
        }
    }

    private fun String.gqlParameterizedPath(): String =
        split('/').joinToString(separator = "/") {
            if (it.startsWith('{') && it.endsWith('}')) {
                "{args." + it.substring(1, it.length - 1).gqlUrlPropertyName() + "}"
            } else {
                it
            }
        }

    private fun KClass<*>.gqlTypeName(): String =
        this.jvmName.substringAfterLast('.')
            .split("$")
            .joinToString(separator = "") {
                it.removePrefix("Api")
                    .removeSuffix("Håndterer")
                    .assertNoÆØÅ()
            }

    private fun String.gqlUrlPropertyName(): String =
        replaceÆØÅ()

    private fun String.replaceÆØÅ(): String =
        replace("æ", "e")
            .replace("ø", "o")
            .replace("å", "a")
            .replace("Æ", "E")
            .replace("Ø", "O")
            .replace("Å", "A")


    private fun String.assertNoÆØÅ(): String =
        apply {
            check(none { it in setOf('æ', 'ø', 'å', 'Æ', 'Ø', 'Å') }) {
                error(
                    "Det er et norsk tegn i \"$this\", det fungerer dessverre fortsatt ikke" +
                            " siden GraphQL-standarden ikke støtter det." +
                            " Unntaket er for URL-parametre siden navnet på dem ikke trenger å matche i frontend" +
                            " (vi erstatter bare æøå der ved generering)."
                )
            }
        }
}
