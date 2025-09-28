package no.nav.helse.spesialist.api.rest.graphqlgenerator

data class GQLRestMutation(
    val operationName: String,
    val fieldName: String,
    val arguments: Map<String, GQLInputType>,
    val fieldType: GQLOutputType,
    val path: String,
) {
    fun toDocument(allOutputTypes: Collection<GQLOutputType>): String {
        val variables = arguments.entries.joinToString(separator = ", ") { (name, type) -> $$"$$$name: $${type.asReference()}" }
        val fieldArguments = arguments.keys.joinToString { $$"$$it: $$$it" }
        return $$"""
            mutation $$operationName($$variables) {
                $$fieldName($$fieldArguments)
                @rest(
                    type: "$${fieldType.asReference()}"
                    endpoint: "spesialist"
                    path: "$$path"
                    method: "POST"
                )""".trimIndent() +
                fieldType.toSelectionSet(
                    indentationLevel = 1,
                    allOutputTypes = allOutputTypes
                ) +
                "\n}"
    }

    fun toMutationObjectField(): String =
        "${fieldName}(${arguments.entries.joinToString(separator = ", ") { (name, type) -> "$name: ${type.asReference()}" }}): ${fieldType.asReference()}"
}
