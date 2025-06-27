package no.nav.helse.db.api

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.db.OppgaveFraDatabaseForVisning
import no.nav.helse.db.OppgavesorteringForDatabase
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.spesialist.api.oppgave.OppgaveForPeriodevisningDto
import java.util.UUID

interface OppgaveApiDao {
    fun finnOppgaveId(fødselsnummer: String): Long?

    fun finnPeriodeoppgave(vedtaksperiodeId: UUID): OppgaveForPeriodevisningDto?

    fun finnOppgaverForVisning(
        ekskluderEgenskaper: List<String>,
        saksbehandlerOid: UUID,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE,
        sortering: List<OppgavesorteringForDatabase> = emptyList(),
        egneSakerPåVent: Boolean = false,
        egneSaker: Boolean = false,
        tildelt: Boolean? = null,
        grupperteFiltrerteEgenskaper: Map<Egenskap.Kategori, List<EgenskapForDatabase>> = emptyMap(),
    ): List<OppgaveFraDatabaseForVisning>

    fun finnFødselsnummer(oppgaveId: Long): String
}
