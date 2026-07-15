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
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.oppgave.OppgaveId
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class InMemoryOppgaveRepository(
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    private val påVentRepository: PåVentRepository,
    private val varselRepository: VarselRepository,
) : OppgaveRepository {
    private val oppgaver = mutableMapOf<OppgaveId, Oppgave>()
    private val oppdatertTidspunkt = mutableMapOf<OppgaveId, LocalDateTime>()

    fun alle(): List<Oppgave> = oppgaver.values.toList()

    override fun lagre(oppgave: Oppgave) {
        oppgaver[oppgave.id] = oppgave
        oppdatertTidspunkt[oppgave.id] = LocalDateTime.now()
    }

    override fun finn(id: Long): Oppgave? = oppgaver[OppgaveId(id)]

    override fun finn(id: OppgaveId): Oppgave? = oppgaver[id]

    override fun finn(id: SpleisBehandlingId): Oppgave? = oppgaver.values.find { it.behandlingId == id }

    override fun finnAktivForPerson(identitetsnummer: Identitetsnummer): Oppgave? {
        val ider = vedtaksperiodeRepository.finnAlleIderForPerson(identitetsnummer)
        return oppgaver.values
            .filter { it.tilstand in listOf(Oppgave.AvventerSaksbehandler) }
            .singleOrNull { it.vedtaksperiodeId in ider }
    }

    override fun finnGjeldendeForPerson(identitetsnummer: Identitetsnummer): Oppgave? {
        val ider = vedtaksperiodeRepository.finnAlleIderForPerson(identitetsnummer)
        return oppgaver.values
            .filter { it.tilstand in listOf(Oppgave.AvventerSystem, Oppgave.AvventerSaksbehandler) }
            .singleOrNull { it.vedtaksperiodeId in ider }
    }

    override fun førsteOpprettetForBehandlingId(behandlingId: UUID): LocalDateTime? = oppgaver.values.filter { it.behandlingId.value == behandlingId }.minOfOrNull { it.opprettet }

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
        ekskluderVarsler: Set<String>,
        tillatteVarsler: Set<String>,
        behandlingOpprettetFom: LocalDate?,
        behandlingOpprettetTom: LocalDate?,
        oppgaveKlarFom: LocalDate?,
        oppgaveKlarTom: LocalDate?,
    ): Side<OppgaveProjeksjon> {
        error("Not implemented for test")
    }

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

    override fun finnAntallOppgaverProjeksjon(saksbehandlersOid: SaksbehandlerOid): OppgaveRepository.AntallOppgaverProjeksjon {
        val mineTildelteOppgaver = oppgaver.values.filter { it.tildeltTil == saksbehandlersOid }
        val mineSakerPåVent = mineTildelteOppgaver.filter { Egenskap.PÅ_VENT in it.egenskaper }
        val mineSakerPåVentNåddFrist =
            mineSakerPåVent.filter { oppgave ->
                val påVent = påVentRepository.finnFor(oppgave.vedtaksperiodeId)
                påVent != null && påVent.frist <= LocalDate.now()
            }
        return OppgaveRepository.AntallOppgaverProjeksjon(
            antallMineSaker = mineTildelteOppgaver.count { Egenskap.PÅ_VENT !in it.egenskaper },
            antallMineSakerPåVent = mineSakerPåVent.size,
            antallMineSakerPåVentNåddFrist = mineSakerPåVentNåddFrist.size,
        )
    }

    override fun finnFødselsnumreForÅpneOppgaverMedAktivtVarsel(varselkode: String): Set<String> =
        oppgaver.values
            .filter { it.tilstand is Oppgave.AvventerSaksbehandler }
            .filter { oppgave ->
                varselRepository
                    .finnVarsler(listOf(oppgave.behandlingId))
                    .any { it.kode == varselkode && it.status == Varsel.Status.AKTIV }
            }.mapNotNull { oppgave -> vedtaksperiodeRepository.finn(oppgave.vedtaksperiodeId)?.identitetsnummer?.value }
            .distinct()
            .toSet()
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
