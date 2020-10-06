package no.nav.helse.modell.vedtak

class WarningDto(
    val melding: String,
    val kilde: WarningKilde
)

enum class WarningKilde { Spesialist, Spleis }
