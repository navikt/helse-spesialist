package no.nav.helse.mediator.oppgave

import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.Sorteringsrekkefølge
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.PåVentId
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.oppgave.OppgaveId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface OppgaveRepository {
    fun lagre(oppgave: Oppgave)

    fun finn(id: Long): Oppgave?

    fun finn(id: OppgaveId): Oppgave?

    fun finn(id: SpleisBehandlingId): Oppgave?

    // finner oppgave i tilstand AvventerSaksbehandler
    fun finnAktivForPerson(identitetsnummer: Identitetsnummer): Oppgave?

    // finner oppgave i tilstand AvventerSaksbehandler eller AvventerSystem
    fun finnGjeldendeForPerson(identitetsnummer: Identitetsnummer): Oppgave?

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

    fun finnListeOppgaveProjeksjoner(
        sidetall: Int,
        sidestørrelse: Int,
    ): Side<OppgaveProjeksjon>

    fun finnBehandledeOppgaveProjeksjoner(
        fom: LocalDate,
        tom: LocalDate,
        sidetall: Int,
        sidestørrelse: Int,
        behandletAvOid: UUID,
    ): Side<BehandletOppgaveProjeksjon>

    fun finnAntallOppgaverProjeksjon(saksbehandlersOid: SaksbehandlerOid): AntallOppgaverProjeksjon

    data class Side<T>(
        val totaltAntall: Long,
        val sidetall: Int,
        val sidestørrelse: Int,
        val elementer: List<T>,
    )

    data class OppgaveProjeksjon(
        val id: Long,
        val identitetsnummer: Identitetsnummer,
        val egenskaper: Set<Egenskap>,
        val tildeltTilOid: SaksbehandlerOid?,
        val opprettetTidspunkt: Instant,
        val behandlingOpprettetTidspunkt: Instant,
        val påVentId: PåVentId?,
    )

    data class BehandletOppgaveProjeksjon(
        val id: Long,
        val fødselsnummer: String,
        val ferdigstiltTidspunkt: LocalDateTime,
        val saksbehandler: String,
        val beslutter: String?,
        val personnavn: PersonnavnFraDatabase,
    )

    data class AntallOppgaverProjeksjon(
        val antallMineSaker: Int,
        val antallMineSakerPåVent: Int,
    )
}
