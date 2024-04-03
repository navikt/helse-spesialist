package no.nav.helse.spesialist.api.oppgave

data class OppgaveForPeriodevisningDto(
    val id: String,
    val kanAvvises: Boolean,
)

enum class Oppgavestatus {
    AvventerSystem,
    AvventerSaksbehandler,
    Invalidert,
    Ferdigstilt,
}
