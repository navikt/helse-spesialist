package no.nav.helse.spesialist.api.rest.graphqlgenerator

data class GQLRestQuery(
    val operationName: String,
    val fieldName: String,
    val arguments: Map<String, GQLTypeReference<GQLScalarType>>,
    val fieldType: GQLTypeReference<GQLOutputType>,
    val path: String,
) {
    fun toDocument(allOutputTypes: Collection<GQLOutputType>): String {
        val variables = arguments.entries.joinToString(separator = ", ") { (name, type) -> $$"$$$name: $$type" }
        val fieldArguments = arguments.keys.joinToString { $$"$$it: $$$it" }
        return """
            query $operationName($variables) {
                $fieldName($fieldArguments)
                @rest(
                    type: "$fieldType"
                    endpoint: "spesialist"
                    path: "$path"
                    method: "GET"
                )""".trimIndent() +
                fieldType.unwrappedType().toSelectionSet(
                    indentationLevel = 1,
                    allOutputTypes = allOutputTypes
                ) +
                "\n}"

    }

    fun toQueryObjectField(): String =
        "${fieldName}(${arguments.entries.joinToString(separator = ", ") { (name, type) -> "$name: $type" }}): $fieldType"
}
