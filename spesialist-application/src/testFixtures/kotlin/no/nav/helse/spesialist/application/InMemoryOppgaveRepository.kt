package no.nav.helse.spesialist.application

import no.nav.helse.db.PersonnavnFraDatabase
import no.nav.helse.db.SorteringsnøkkelForDatabase
import no.nav.helse.db.Sorteringsrekkefølge
import no.nav.helse.mediator.oppgave.OppgaveRepository
import no.nav.helse.mediator.oppgave.OppgaveRepository.BehandletOppgaveProjeksjon
import no.nav.helse.mediator.oppgave.OppgaveRepository.OppgaveProjeksjon
import no.nav.helse.mediator.oppgave.OppgaveRepository.Side
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.oppgave.OppgaveId
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class InMemoryOppgaveRepository : OppgaveRepository {
    private val oppgaver = mutableMapOf<OppgaveId, Oppgave>()
    private val oppdatertTidspunkt = mutableMapOf<OppgaveId, LocalDateTime>()

    fun alle(): List<Oppgave> = oppgaver.values.toList()

    override fun lagre(oppgave: Oppgave) {
        oppgaver[oppgave.id] = oppgave
        oppdatertTidspunkt[oppgave.id] = LocalDateTime.now()
    }

    override fun finn(id: Long): Oppgave? = oppgaver[OppgaveId(id)]

    override fun finn(id: SpleisBehandlingId): Oppgave? = oppgaver.values.find { it.behandlingId == id.value }

    override fun finnAktivForPerson(identitetsnummer: Identitetsnummer): Oppgave? {
        error("Not implemented for this test")
    }

    override fun førsteOpprettetForBehandlingId(behandlingId: UUID): LocalDateTime? = oppgaver.values.filter { it.behandlingId == behandlingId }.minOfOrNull { it.opprettet }

    override fun finnOppgaveProjeksjoner(
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
    ): Side<OppgaveProjeksjon> {
        error("Not implemented for this test")
    }

    override fun finnListeOppgaveProjeksjoner(
        sidetall: Int,
        sidestørrelse: Int,
    ): Side<OppgaveProjeksjon> =
        Side(
            totaltAntall = oppgaver.size.toLong(),
            sidetall = sidetall,
            sidestørrelse = sidestørrelse,
            elementer = oppgaver.values.map { it.toOppgaveProjeksjon() },
        )

    override fun finnBehandledeOppgaveProjeksjoner(
        fom: LocalDate,
        tom: LocalDate,
        sidetall: Int,
        sidestørrelse: Int,
        behandletAvOid: UUID,
    ) = Side(
        totaltAntall = oppgaver.keys.first().value,
        sidetall = sidetall,
        sidestørrelse = sidestørrelse,
        elementer = oppgaver.values.map { it.toBehandletOppgaveProjeksjon() },
    )

    override fun finnAntallOppgaverProjeksjon(saksbehandlersOid: SaksbehandlerOid): OppgaveRepository.AntallOppgaverProjeksjon =
        OppgaveRepository.AntallOppgaverProjeksjon(
            antallMineSaker = oppgaver.values.count { it.tildeltTil == saksbehandlersOid && Egenskap.PÅ_VENT !in it.egenskaper },
            antallMineSakerPåVent = oppgaver.values.count { it.tildeltTil == saksbehandlersOid && Egenskap.PÅ_VENT in it.egenskaper },
        )
}

private fun Oppgave.toBehandletOppgaveProjeksjon(): BehandletOppgaveProjeksjon {
    // Denne skal egentlig hente data fra oppgave, men det blir for knotete siden oppgave ikke er skrevet om til DDD ennå.
    return BehandletOppgaveProjeksjon(
        id = id.value,
        fødselsnummer = "42",
        ferdigstiltTidspunkt = LocalDateTime.of(2020, 2, 2, 12, 30),
        saksbehandler = "her skal saksbehandlers navn stå",
        beslutter = "her skal beslutters navn eventuelt stå",
        personnavn =
            PersonnavnFraDatabase(
                fornavn = "navnet",
                mellomnavn = "til",
                etternavn = "personen",
            ),
    )
}

private fun Oppgave.toOppgaveProjeksjon(): OppgaveProjeksjon {
    // Denne skal egentlig hente data fra oppgave, men det blir for knotete siden oppgave ikke er skrevet om til DDD ennå.
    return OppgaveProjeksjon(
        id = id.value,
        identitetsnummer = Identitetsnummer.fraString("42"),
        egenskaper = emptySet(),
        tildeltTilOid = null,
        opprettetTidspunkt = LocalDateTime.of(2020, 2, 2, 12, 30).toInstant(ZoneOffset.UTC),
        behandlingOpprettetTidspunkt = LocalDateTime.of(2020, 2, 2, 12, 30).toInstant(ZoneOffset.UTC),
        påVentId = null,
    )
}
