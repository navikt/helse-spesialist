package no.nav.helse.spesialist.api.rest.graphqlgenerator

sealed interface TypeDefinition {
    val name: String
}

sealed interface InputTypeDefinition : TypeDefinition
sealed interface OutputTypeDefinition : TypeDefinition

data class InputObjectTypeDefinition(
    override val name: String,
    val properties: Map<String, TypeReference>
) : InputTypeDefinition {
    fun toSchemaType(): String = buildString {
        append("input ")
        append(name)
        append(" {\n")
        append(properties.entries.joinToString("\n", postfix = "\n") { (name, type) -> "    $name: $type" })
        append("}\n")
    }
}

data class OutputObjectTypeDefinition(
    override val name: String,
    val implements: List<TypeDefinition>,
    val isInterface: Boolean,
    val properties: Map<String, TypeReference>
) : OutputTypeDefinition {
    fun toSchemaType(): String = buildString {
        append(if (isInterface) "interface" else "type")
        append(" ")
        append(name)
        if (implements.isNotEmpty()) {
            append(" implements ")
            append(implements.joinToString(" & ") { it.name })
        }
        append(" {\n")
        append(properties.entries.joinToString("\n", postfix = "\n") { (name, type) -> "    $name: $type" })
        append("}\n")
    }
}

data class InputCollectionTypeDefinition(
    val elementType: TypeReference,
) : InputTypeDefinition {
    override val name: String = "[${elementType}]"
}

data class OutputCollectionTypeDefinition(
    val elementType: TypeReference,
) : OutputTypeDefinition {
    override val name: String = "[${elementType}]"
}

data class ScalarTypeDefinition(
    override val name: String
) : TypeDefinition
