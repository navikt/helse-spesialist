package no.nav.helse.modell.stoppautomatiskbehandling

import kotliquery.TransactionalSession
import no.nav.helse.db.NotatRepository
import no.nav.helse.db.OppgaveRepository
import no.nav.helse.db.PeriodehistorikkRepository
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.db.StansAutomatiskBehandlingRepository
import no.nav.helse.db.TransactionalNotatDao
import no.nav.helse.db.TransactionalOppgaveDao
import no.nav.helse.db.TransactionalPeriodehistorikkDao
import no.nav.helse.db.TransactionalStansAutomatiskBehandlingDao
import no.nav.helse.db.TransactionalUtbetalingDao
import no.nav.helse.db.UtbetalingRepository
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.Personhandling
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.AKTIVITETSKRAV
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.BESTRIDELSE_SYKMELDING
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MANGLENDE_MEDVIRKING
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MEDISINSK_VILKAR
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingStansAutomatiskBehandling
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_UAVKLART
import no.nav.helse.modell.vilkårsprøving.SubsumsjonEvent
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.graphql.schema.UnntattFraAutomatiskGodkjenning
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class StansAutomatiskBehandlingMediator(
    private val stansAutomatiskBehandlingRepository: StansAutomatiskBehandlingRepository,
    private val periodehistorikkRepository: PeriodehistorikkRepository,
    private val oppgaveRepository: OppgaveRepository,
    private val utbetalingRepository: UtbetalingRepository,
    private val notatRepository: NotatRepository,
    private val subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
) : StansAutomatiskBehandlinghåndterer {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    private val subsumsjonsmelder by lazy { subsumsjonsmelderProvider() }

    internal fun håndter(
        handling: Personhandling,
        saksbehandler: Saksbehandler,
    ) {
        stansAutomatiskBehandlingRepository.lagreFraSpeil(handling.gjelderFødselsnummer())
        lagreNotat(handling.gjelderFødselsnummer(), handling.begrunnelse(), saksbehandler.oid())
    }

    internal fun håndter(
        fødselsnummer: String,
        status: String,
        årsaker: Set<StoppknappÅrsak>,
        opprettet: LocalDateTime,
        originalMelding: String,
        kilde: String,
    ) {
        stansAutomatiskBehandlingRepository.lagreFraISyfo(
            fødselsnummer = fødselsnummer,
            status = status,
            årsaker = årsaker,
            opprettet = opprettet,
            originalMelding = originalMelding,
            kilde = kilde,
        )
        lagrePeriodehistorikk(fødselsnummer)
    }

    override fun unntattFraAutomatiskGodkjenning(fødselsnummer: String): UnntattFraAutomatiskGodkjenning =
        stansAutomatiskBehandlingRepository.hentFor(fødselsnummer).filtrerGjeldendeStopp().tilUnntattFraAutomatiskGodkjenning()

    override fun sjekkOmAutomatiseringErStanset(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
    ): Boolean {
        val stoppmeldinger =
            stansAutomatiskBehandlingRepository.hentFor(fødselsnummer).filtrerGjeldendeStopp().map {
                StoppknappmeldingForSubsumsjon(it.årsaker, it.meldingId!!)
            }
        sendSubsumsjonMeldinger(stoppmeldinger, fødselsnummer, vedtaksperiodeId, organisasjonsnummer)
        return stoppmeldinger.isNotEmpty()
    }

    override fun nyStansAutomatiskBehandlinghåndterer(transactionalSession: TransactionalSession): StansAutomatiskBehandlinghåndterer {
        return StansAutomatiskBehandlingMediator(
            TransactionalStansAutomatiskBehandlingDao(transactionalSession),
            TransactionalPeriodehistorikkDao(transactionalSession),
            TransactionalOppgaveDao(transactionalSession),
            TransactionalUtbetalingDao(transactionalSession),
            TransactionalNotatDao(transactionalSession),
            subsumsjonsmelderProvider,
        )
    }

    private fun lagrePeriodehistorikk(fødselsnummer: String) {
        val utbetalingId =
            oppgaveRepository.finnOppgaveId(fødselsnummer)?.let { oppgaveRepository.finnUtbetalingId(it) }
                ?: utbetalingRepository.sisteUtbetalingIdFor(fødselsnummer)
        if (utbetalingId != null) {
            periodehistorikkRepository.lagre(STANS_AUTOMATISK_BEHANDLING, null, utbetalingId, null)
        } else {
            sikkerlogg.error("Fant ikke oppgave for $fødselsnummer. Fikk ikke lagret historikkinnslag om stans av automatisk behandling")
        }
    }

    private fun lagreNotat(
        fødselsnummer: String,
        begrunnelse: String,
        saksbehandlerOid: UUID,
    ) = try {
        val oppgaveId = fødselsnummer.finnOppgaveId()
        notatRepository.lagreForOppgaveId(oppgaveId, begrunnelse, saksbehandlerOid, NotatType.OpphevStans)
    } catch (e: Exception) {
        sikkerlogg.error("Fant ikke oppgave for $fødselsnummer. Fikk ikke lagret notat om oppheving av stans")
    }

    private fun String.finnOppgaveId() = oppgaveRepository.finnOppgaveId(this) ?: oppgaveRepository.finnOppgaveIdUansettStatus(this)

    private fun List<StansAutomatiskBehandlingFraDatabase>.filtrerGjeldendeStopp(): List<StansAutomatiskBehandlingFraDatabase> {
        val gjeldende = mutableListOf<StansAutomatiskBehandlingFraDatabase>()
        sortedWith { a, b ->
            a.opprettet.compareTo(b.opprettet)
        }.forEach {
            when (it.status) {
                "STOPP_AUTOMATIKK" -> gjeldende += it
                "NORMAL" -> gjeldende.clear()
                else -> {
                    logg.error("Ukjent status-type: {}", it.status)
                    gjeldende += it
                }
            }
        }
        return gjeldende
    }

    private fun List<StansAutomatiskBehandlingFraDatabase>.tilUnntattFraAutomatiskGodkjenning() =
        if (isEmpty()) {
            UnntattFraAutomatiskGodkjenning(
                erUnntatt = false,
                arsaker = emptyList(),
                tidspunkt = null,
            )
        } else {
            UnntattFraAutomatiskGodkjenning(
                erUnntatt = true,
                arsaker = flatMap { it.årsaker.map(StoppknappÅrsak::name) },
                tidspunkt = last().opprettet,
            )
        }

    private fun sendSubsumsjonMeldinger(
        stoppmeldinger: List<StoppknappmeldingForSubsumsjon>,
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
    ) = stoppmeldinger.byggSubsumsjonEventer(fødselsnummer, vedtaksperiodeId, organisasjonsnummer).toMutableList()
        .apply {
            if (none { it.paragraf == "8-4" }) {
                add(åtteFireOppfyltEvent(fødselsnummer, vedtaksperiodeId, organisasjonsnummer))
            }
        }.forEach {
            subsumsjonsmelder.nySubsumsjon(fødselsnummer, it)
        }

    private fun List<StoppknappmeldingForSubsumsjon>.byggSubsumsjonEventer(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
    ): List<SubsumsjonEvent> =
        tilLovhjemler().map { (meldingId, årsak, lovhjemmel) ->
            subsumsjonEvent(fødselsnummer, vedtaksperiodeId, organisasjonsnummer, årsak, meldingId, lovhjemmel)
        }

    private fun subsumsjonEvent(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
        årsak: StoppknappÅrsak,
        meldingId: String,
        lovhjemmel: Lovhjemmel,
    ) = Subsumsjon(
        lovhjemmel = lovhjemmel,
        fødselsnummer = fødselsnummer,
        input = mapOf("syfostopp" to true, "årsak" to årsak),
        output = emptyMap(),
        utfall = VILKAR_UAVKLART,
        sporing =
            SporingStansAutomatiskBehandling(
                listOf(vedtaksperiodeId),
                listOf(organisasjonsnummer),
                listOf(meldingId),
            ),
    ).byggEvent()

    private fun åtteFireOppfyltEvent(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
    ): SubsumsjonEvent =
        Subsumsjon(
            lovhjemmel = Lovhjemmel("8-4", "1", null, "folketrygdloven", "2021-05-21"),
            fødselsnummer = fødselsnummer,
            input = emptyMap(),
            output = emptyMap(),
            utfall = VILKAR_OPPFYLT,
            sporing =
                SporingStansAutomatiskBehandling(
                    listOf(vedtaksperiodeId),
                    listOf(organisasjonsnummer),
                    emptyList(),
                ),
        ).byggEvent()

    private fun List<StoppknappmeldingForSubsumsjon>.tilLovhjemler() =
        map { melding ->
            melding.årsaker.map {
                val lovhjemmel =
                    when (it) {
                        MEDISINSK_VILKAR -> Lovhjemmel("8-4", "1", null, "folketrygdloven", "2021-05-21")
                        BESTRIDELSE_SYKMELDING -> Lovhjemmel("8-4", "1", null, "folketrygdloven", "2021-05-21")
                        AKTIVITETSKRAV -> Lovhjemmel("8-8", "2", null, "folketrygdloven", "2021-05-21")
                        MANGLENDE_MEDVIRKING -> Lovhjemmel("8-8", "1", null, "folketrygdloven", "2021-05-21")
                    }
                Triple(melding.meldingId, it, lovhjemmel)
            }
        }.flatten()

    private class StoppknappmeldingForSubsumsjon(
        val årsaker: Set<StoppknappÅrsak>,
        val meldingId: String,
    )
}
