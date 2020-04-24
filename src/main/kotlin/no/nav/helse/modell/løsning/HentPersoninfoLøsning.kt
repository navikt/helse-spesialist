package no.nav.helse.modell.løsning

internal class HentPersoninfoLøsning(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String
)

internal enum class PersonEgenskap(private val diskresjonskode: String) {
    Kode6("SPSF"), Kode7("SPFO"); // TODO: Hvilke fler egenskaper kan man ha?
    companion object {
        internal fun find(diskresjonskode: String?) = values().firstOrNull { it.diskresjonskode == diskresjonskode }
    }
}
