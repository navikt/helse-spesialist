package no.nav.helse.spesialist.api.rest.graphqlgenerator

data class GQLRestMutation(
    val operationName: String,
    val fieldName: String,
    val arguments: Map<String, GQLTypeReference<GQLScalarType>>,
    val inputArgument: GQLTypeReference<GQLInputType>,
    val fieldType: GQLTypeReference<GQLOutputType>,
    val path: String,
) {
    private fun allArguments(): Map<String, GQLTypeReference<out GQLInputType>> =
        arguments.plus("input" to inputArgument)

    fun toDocument(allOutputTypes: Collection<GQLOutputType>): String {
        val variables = allArguments().entries.joinToString(separator = ", ") { (name, type) -> $$"$$$name: $$type" }
        val fieldArguments = allArguments().keys.joinToString { $$"$$it: $$$it" }
        return $$"""
            mutation $$operationName($$variables) {
                $$fieldName($$fieldArguments)
                @rest(
                    type: "$$fieldType"
                    endpoint: "spesialist"
                    path: "$$path"
                    method: "POST"
                )""".trimIndent() +
                fieldType.unwrappedType().toSelectionSet(
                    indentationLevel = 1,
                    allOutputTypes = allOutputTypes
                ) +
                "\n}"
    }

    fun toMutationObjectField(): String =
        "${fieldName}(${allArguments().entries.joinToString(separator = ", ") { (name, type) -> "$name: $type" }}): $fieldType"
}
