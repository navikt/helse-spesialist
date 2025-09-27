package no.nav.helse.spesialist.api.rest.graphqlgenerator

sealed interface GQLType {
    val name: String
    fun toSDL(): String
}

sealed interface GQLInputType : GQLType
sealed interface GQLOutputType : GQLType {
    fun toSelectionSet(indentationLevel: Int, allOutputTypes: Collection<GQLOutputType>): String
}

sealed interface GQLObjectOrInterfaceType : GQLOutputType {
    override val name: String
    val implementedInterfaces: List<GQLInterfaceType>
    val fields: Map<String, GQLTypeReference<GQLOutputType>>
}
