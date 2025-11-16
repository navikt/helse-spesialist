package no.nav.helse.spesialist.domain.ddd

abstract class Entity<IDTYPE>(
    val id: IDTYPE,
) {
    override fun equals(other: Any?): Boolean = other != null && this::class == other::class && this.id == (other as Entity<*>).id

    override fun hashCode(): Int = 31 * id.hashCode() + javaClass.hashCode()
}
