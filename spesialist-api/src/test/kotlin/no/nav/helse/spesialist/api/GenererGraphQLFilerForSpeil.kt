package no.nav.helse.spesialist.api

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.GetTilkomneInntektskilderHåndterer
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.jvmName

val ALLE_HÅNDTERERE = listOf(GetTilkomneInntektskilderHåndterer())

fun main() {
    val generatedTypes = mutableMapOf<KClass<*>, String>()
    val generatedQueries = mutableListOf<String>()
    ALLE_HÅNDTERERE.forEach { håndterer ->
        val queryName = håndterer::class.simpleName!!.removeSuffix("Håndterer")
        val parameters =
            håndterer::extractParametre.returnType.classifierAsClass().declaredMemberProperties
                .map { property -> property.name.gqlPropertyName() to property.returnType.gqlTypeName() }
        val responseBodyType = håndterer.getResponseBodyType()

        generateTypesRecursively(responseBodyType.asFlatClass(), generatedTypes)

        val propertyTree = responseBodyType.toQueryPropertyTree()

        println(generatedTypes.values.joinToString(separator = "\n"))
        val query = """
            query REST$queryName(${parameters.joinToString(separator = ", ") { (name, type) -> "\$$name: $type" }}) {
                rest$queryName(${parameters.map { it.first }.joinToString { "$it: \$$it" }})
                @rest(
                    type: "${responseBodyType.gqlTypeName()}"
                    endpoint: "spesialist"
                    path: "/${håndterer.urlPath.gqlParameterizedPath()}"
                    method: "GET"
                )""".trimIndent() + propertyTree.toString(2) + "\n}"
        generatedQueries.add("rest$queryName(${parameters.joinToString(separator = ", ") { (name, type) -> "$name: $type" }}): ${responseBodyType.gqlTypeName()}")
        File("../helse-speil/src/io/graphql/rest/spesialist/$queryName.query.graphql").writeText(query)
        println(query)
    }
    val restSchema = buildString {
        generatedTypes.map { (_, definition) -> definition }.reversed().forEach { definition ->
            append(definition)
            append("\n")
        }
        append("extend type Query {\n")
        append(generatedQueries.joinToString("    \n", prefix = "    ", postfix = "\n"))
        append("}\n")
    }
    File("../helse-speil/src/io/graphql/rest/spesialist/schema.graphql").writeText(restSchema)
}

private fun KType.classifierAsClass(): KClass<*> =
    classifier as KClass<*>

class QueryPropertyTree(private val map: Map<String, QueryPropertyTree>) {
    fun isEmpty(): Boolean = map.isEmpty()

    fun toString(indentLevel: Int): String = buildString {
        val filteredMap =
            map.filterNot { (propertyName, subTree) -> propertyName.startsWith("... on ") && subTree.isEmpty() }
        if (filteredMap.isNotEmpty()) {
            append(" {")
            append("\n")
            filteredMap.forEach { (propertyName, subTree) ->
                append((1..indentLevel).joinToString(separator = "") { "    " })
                append(propertyName)
                append(subTree.toString(indentLevel + 1))
                append("\n")
            }
            append((1..<indentLevel).joinToString(separator = "") { "    " })
            append("}")
        }
    }
}

private fun KType.toQueryPropertyTree(): QueryPropertyTree {
    val referencedClass = asFlatClass()
    return QueryPropertyTree(
        if (isScalarType(referencedClass)) {
            emptyMap()
        } else {
            referencedClass.declaredMemberProperties
                .filter { it.findAnnotations(GraphQLIgnore::class).isEmpty() }
                .filterNot { property -> referencedClass.superclasses.any { it.declaredMemberProperties.any { superProperty -> superProperty.name == property.name } } }
                .map { it.name to it.returnType.toQueryPropertyTree() }
                .plus(
                    referencedClass.sealedSubclasses.map { sealedSubclass ->
                        "... on ${sealedSubclass.gqlTypeName()}" to sealedSubclass.starProjectedType.toQueryPropertyTree()
                    }
                ).toMap()
        })
}

private fun KType.asFlatClass(): KClass<out Any> =
    if (isCollectionType(classifierAsClass())) {
        arguments.single().type!!.classifierAsClass()
    } else {
        classifierAsClass()
    }

private fun generateTypesRecursively(type: KClass<*>, generatedTypes: MutableMap<KClass<*>, String>) {
    generatedTypes[type] = buildString {
        if (type.java.isInterface) {
            append("interface ")
        } else {
            append("type ")
        }
        append(type.gqlTypeName())
        val superclasses = type.superclasses.filterNot { it == Any::class }
        if (superclasses.size > 1) {
            error(
                "$type extender mer enn én klasse / interface (${superclasses.joinToString()})." +
                        " Det er ikke implementert støtte for dette i denne genereringen." +
                        " Det er heller ikke undersøkt ordentlig om GraphQL i det hele tatt støtter det."
            )
        }
        if (superclasses.singleOrNull() != null) {
            append(" implements ")
            append(superclasses.single().gqlTypeName())
        }
        append(" {\n")
        append(
            type.declaredMemberProperties
                .filter { it.findAnnotations(GraphQLIgnore::class).isEmpty() }
                .joinToString("\n", postfix = "\n") { property ->
                    "    ${property.name}: ${property.returnType.gqlTypeName()}"
                })
        append("}\n")
    }
    type.declaredMemberProperties
        .filter { it.findAnnotations(GraphQLIgnore::class).isEmpty() }
        .map { it.returnType }
        .flatMap { listOf((it.classifier as KClass<*>)) + it.arguments.map { it.type!!.classifier as KClass<*> } }
        .flatMap { listOf(it) + it.sealedSubclasses }
        .filterNot { isScalarType(it) }
        .filterNot { isCollectionType(it) }
        .forEach { type ->
            if (!generatedTypes.containsKey(type)) {
                generateTypesRecursively(type, generatedTypes)
            }
        }
}

private fun isScalarType(klass: KClass<*>): Boolean = klass.simpleName in setOf(
    "String",
    "Boolean",
    "Int",
    "Short",
    "Long",
    "Float",
    "Double",
    "BigDecimal",
    "LocalDate",
    "LocalDateTime",
    "Instant",
    "UUID"
)

private fun isCollectionType(klass: KClass<*>): Boolean = klass in setOf(
    List::class,
    Set::class
)

private fun String.gqlParameterizedPath(): String =
    split('/').joinToString(separator = "/") {
        if (it.startsWith('{') && it.endsWith('}')) {
            "{args." + it.substring(1, it.length - 1).gqlPropertyName() + "}"
        } else {
            it
        }
    }

private fun KType.gqlTypeName(): String =
    if (isCollectionType(classifier as KClass<*>)) {
        "[" + arguments.single().type!!.gqlTypeName() + "]"
    } else {
        (classifier!! as KClass<*>).gqlTypeName()
    } + gqlNullability()

private fun KClass<*>.gqlTypeName(): String =
    if (isScalarType(this)) {
        simpleName!!
    } else {
        this.jvmName.substringAfterLast('.').removePrefix("Api").replace("$", "")
    }

private fun String.gqlPropertyName(): String =
    replace("æ", "e")
        .replace("ø", "o")
        .replace("å", "a")
        .replace("Æ", "E")
        .replace("Ø", "O")
        .replace("Å", "A")

private fun KType.gqlNullability(): String = if (isMarkedNullable) "" else "!"
