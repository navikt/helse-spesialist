package no.nav.helse.spesialist.domain.ddd

abstract class LateIdEntity<IDTYPE>(
    private var id: IDTYPE?,
) {
    fun harFåttTildeltId() = id != null

    fun tildelId(id: IDTYPE) {
        if (this.id != null) error("Prøvde å tildele ny ID til entity som allerede har ID")
        this.id = id
    }

    fun id() =
        id ?: error(
            "Denne entity'en har ikke fått tildelt noen ID ennå." +
                " ${::harFåttTildeltId.name}() kan brukes for å sjekke om den har det.",
        )

    override fun equals(other: Any?): Boolean = other != null && this::class == other::class && this.id == (other as LateIdEntity<*>).id

    override fun hashCode(): Int = 31 * id.hashCode() + javaClass.hashCode()
}
