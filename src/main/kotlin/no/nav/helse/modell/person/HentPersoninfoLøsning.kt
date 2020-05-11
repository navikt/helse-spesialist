package no.nav.helse.modell.person

import java.time.LocalDate

internal class HentPersoninfoLøsning(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String,
    internal val fødselsdato: LocalDate,
    internal val kjønn: Kjønn
)

internal enum class Kjønn { Mann, Kvinne, Ukjent }

internal enum class PersonEgenskap(private val diskresjonskode: String) {
    Kode6("SPSF"), Kode7("SPFO"); // TODO: Hvilke fler egenskaper kan man ha?
    companion object {
        internal fun find(diskresjonskode: String?) = values().firstOrNull { it.diskresjonskode == diskresjonskode }
    }
}
