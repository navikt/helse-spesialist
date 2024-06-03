package no.nav.helse.modell.stoppautomatiskbehandling

import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.mediator.oppgave.OppgaveDao
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.Personhandling
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.AKTIVITETSKRAV
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.BESTRIDELSE_SYKMELDING
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MANGLENDE_MEDVIRKING
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MEDISINSK_VILKAR
import no.nav.helse.modell.utbetaling.UtbetalingDao
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingStansAutomatiskBehandling
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_UAVKLART
import no.nav.helse.modell.vilkårsprøving.SubsumsjonEvent
import no.nav.helse.spesialist.api.StansAutomatiskBehandlinghåndterer
import no.nav.helse.spesialist.api.graphql.schema.NotatType
import no.nav.helse.spesialist.api.graphql.schema.UnntattFraAutomatiskGodkjenning
import no.nav.helse.spesialist.api.notat.NotatMediator
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkDao
import no.nav.helse.spesialist.api.periodehistorikk.PeriodehistorikkType.STANS_AUTOMATISK_BEHANDLING
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class StansAutomatiskBehandlingMediator(
    private val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val oppgaveDao: OppgaveDao,
    private val utbetalingDao: UtbetalingDao,
    private val notatMediator: NotatMediator,
    private val subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
) : StansAutomatiskBehandlinghåndterer {
    private val logg = LoggerFactory.getLogger(this::class.java)
    private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

    private val subsumsjonsmelder by lazy { subsumsjonsmelderProvider() }

    internal fun håndter(
        handling: Personhandling,
        saksbehandler: Saksbehandler,
    ) {
        stansAutomatiskBehandlingDao.lagre(
            fødselsnummer = handling.gjelderFødselsnummer(),
            status = "NORMAL",
            årsaker = emptySet(),
            opprettet = LocalDateTime.now(),
            originalMelding = null,
            kilde = "SPEIL",
        )
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
        stansAutomatiskBehandlingDao.lagre(
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
        stansAutomatiskBehandlingDao.hentFor(fødselsnummer).filtrerGjeldendeStopp().tilUnntattFraAutomatiskGodkjenning()

    override fun sjekkOmAutomatiseringErStanset(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
    ): Boolean {
        val stoppmeldinger =
            stansAutomatiskBehandlingDao.hentFor(fødselsnummer).filtrerGjeldendeStopp().map {
                StoppknappmeldingForSubsumsjon(it.årsaker, it.meldingId!!)
            }
        sendSubsumsjonMeldinger(stoppmeldinger, fødselsnummer, vedtaksperiodeId, organisasjonsnummer)
        return stoppmeldinger.isNotEmpty()
    }

    private fun lagrePeriodehistorikk(fødselsnummer: String) {
        val utbetalingId =
            oppgaveDao.finnOppgaveId(fødselsnummer)?.let { oppgaveDao.finnUtbetalingId(it) }
                ?: utbetalingDao.sisteUtbetalingIdFor(fødselsnummer)
        if (utbetalingId != null) {
            periodehistorikkDao.lagre(STANS_AUTOMATISK_BEHANDLING, null, utbetalingId, null)
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
        notatMediator.lagreForOppgaveId(oppgaveId, begrunnelse, saksbehandlerOid, NotatType.OpphevStans)
    } catch (e: Exception) {
        sikkerlogg.error("Fant ikke oppgave for $fødselsnummer. Fikk ikke lagret notat om oppheving av stans")
    }

    private fun String.finnOppgaveId() = oppgaveDao.finnOppgaveId(this) ?: oppgaveDao.finnOppgaveIdUansettStatus(this)

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
                tidspunkt = last().opprettet.format(DateTimeFormatter.ISO_DATE_TIME),
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
