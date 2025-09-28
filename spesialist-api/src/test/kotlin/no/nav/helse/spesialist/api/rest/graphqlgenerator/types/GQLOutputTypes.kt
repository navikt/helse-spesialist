package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

sealed interface GQLOutputType : GQLType {
    fun toSelectionSet(allOutputTypes: Collection<GQLOutputType>): String
}

sealed interface GQLNamedOrListOutputType: GQLOutputType

sealed interface GQLNamedOutputType : GQLNamedType, GQLOutputType, GQLNamedOrListOutputType

sealed interface GQLWrappedOutputType : GQLOutputType {
    fun unwrap(): GQLNamedOutputType
    override fun toSelectionSet(allOutputTypes: Collection<GQLOutputType>): String =
        unwrap().toSelectionSet(allOutputTypes)
}

class GQLListOutputType(val itemType: GQLOutputType) : GQLWrappedOutputType, GQLNamedOrListOutputType {
    override fun unwrap(): GQLNamedOutputType = when (itemType) {
        is GQLNamedOutputType -> itemType
        is GQLWrappedOutputType -> itemType.unwrap()
    }
    override fun asReference() = "[${itemType.asReference()}]"
}

class GQLNonNullOutputType(val wrappedType: GQLNamedOrListOutputType) : GQLWrappedOutputType {
    override fun unwrap(): GQLNamedOutputType = when (wrappedType) {
        is GQLNamedOutputType -> wrappedType
        is GQLWrappedOutputType -> wrappedType.unwrap()
    }
    override fun asReference() = wrappedType.asReference() + "!"
}

sealed interface GQLObjectOrInterfaceType : GQLNamedOutputType {
    val implementedInterfaces: List<GQLInterfaceType>
    val fields: Map<String, GQLOutputType>
}
