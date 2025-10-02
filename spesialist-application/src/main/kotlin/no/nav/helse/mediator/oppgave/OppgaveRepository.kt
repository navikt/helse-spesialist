package no.nav.helse.mediator.oppgave

import no.nav.helse.db.PaVentInfoFraDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.Sorteringsrekkefølge
import no.nav.helse.modell.oppgave.Egenskap
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface OppgaveRepository {
    fun lagre(oppgave: Oppgave)

    fun finn(id: Long): Oppgave?

    // TODO: Helst bør vi bruke finn(), men Oppgave er ikke et aggregat ennå
    fun finnSisteOppgaveForUtbetaling(utbetalingId: UUID): OppgaveTilstandStatusOgGodkjenningsbehov?

    fun førsteOpprettetForBehandlingId(behandlingId: UUID): LocalDateTime?

    fun finnOppgaveProjeksjoner(
        minstEnAvEgenskapene: List<Set<Egenskap>>,
        ingenAvEgenskapene: Set<Egenskap>,
        erTildelt: Boolean?,
        tildeltTilOid: SaksbehandlerOid?,
        erPåVent: Boolean?,
        ikkeSendtTilBeslutterAvOid: SaksbehandlerOid?,
        sorterPå: SorteringsnøkkelForDatabase,
        sorteringsrekkefølge: Sorteringsrekkefølge,
        sidetall: Int,
        sidestørrelse: Int,
    ): Side<OppgaveProjeksjon>

    data class Side<T>(
        val totaltAntall: Long,
        val sidetall: Int,
        val sidestørrelse: Int,
        val elementer: List<T>,
    )

    data class OppgaveProjeksjon(
        val id: Long,
        val aktørId: String,
        val navn: Personnavn,
        val egenskaper: Set<Egenskap>,
        val tildeltTilOid: SaksbehandlerOid?,
        val opprettetTidspunkt: Instant,
        val opprinneligSøknadstidspunkt: Instant,
        val påVentFrist: LocalDate?,
        val påVentInfo: PaVentInfoFraDatabase?,
    ) {
        data class Personnavn(
            val fornavn: String,
            val mellomnavn: String?,
            val etternavn: String,
        )
    }

    data class OppgaveTilstandStatusOgGodkjenningsbehov(
        val id: Long,
        val tilstand: Oppgave.Tilstand,
        val godkjenningsbehovId: UUID,
        val utbetalingId: UUID,
    )
}
