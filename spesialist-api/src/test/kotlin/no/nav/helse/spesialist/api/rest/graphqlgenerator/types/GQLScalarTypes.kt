package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

sealed interface GQLScalarType : GQLNamedInputType, GQLNamedOutputType {
    override fun toSelectionSet(allOutputTypes: Collection<GQLOutputType>): String = ""
}

class GQLCustomScalarType(override val name: String) : GQLScalarType {
    override fun toSDL(): String = "scalar $name"
}

class GQLDefaultScalarType(override val name: String) : GQLScalarType {
    override fun toSDL(): String = ""
}
