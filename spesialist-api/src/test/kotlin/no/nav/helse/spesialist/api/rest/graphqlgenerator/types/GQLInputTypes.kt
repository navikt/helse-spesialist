package no.nav.helse.spesialist.api.rest.graphqlgenerator.types

sealed interface GQLInputType : GQLType

sealed interface GQLNamedOrListInputType: GQLInputType

sealed interface GQLNamedInputType : GQLNamedType, GQLInputType, GQLNamedOrListInputType

sealed interface GQLWrappedInputType : GQLInputType {
    fun unwrap(): GQLNamedInputType
}

class GQLListInputType(val itemType: GQLInputType) : GQLWrappedInputType, GQLNamedOrListInputType {
    override fun unwrap(): GQLNamedInputType = when (itemType) {
        is GQLNamedInputType -> itemType
        is GQLWrappedInputType -> itemType.unwrap()
    }
    override fun asReference() = "[${itemType.asReference()}]"
}

class GQLNonNullInputType(val wrappedType: GQLNamedOrListInputType) : GQLWrappedInputType {
    override fun unwrap(): GQLNamedInputType = when (wrappedType) {
        is GQLNamedInputType -> wrappedType
        is GQLWrappedInputType -> wrappedType.unwrap()
    }
    override fun asReference() = wrappedType.asReference() + "!"
}
