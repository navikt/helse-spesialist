package no.nav.helse.modell.stoppautomatiskbehandling

import no.nav.helse.AutomatiseringStansetSjekker
import no.nav.helse.db.OppgaveDao
import no.nav.helse.db.PeriodehistorikkDao
import no.nav.helse.db.SessionContext
import no.nav.helse.db.StansAutomatiskBehandlingDao
import no.nav.helse.db.StansAutomatiskBehandlingFraDatabase
import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.periodehistorikk.Historikkinnslag
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.AKTIVITETSKRAV
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.BESTRIDELSE_SYKMELDING
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MANGLENDE_MEDVIRKING
import no.nav.helse.modell.stoppautomatiskbehandling.StoppknappÅrsak.MEDISINSK_VILKAR
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingStansAutomatiskBehandling
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_UAVKLART
import no.nav.helse.spesialist.application.logg.logg
import no.nav.helse.spesialist.application.logg.sikkerlogg
import java.util.UUID

class StansAutomatiskBehandlingMediator(
    private val stansAutomatiskBehandlingDao: StansAutomatiskBehandlingDao,
    private val periodehistorikkDao: PeriodehistorikkDao,
    private val oppgaveDao: OppgaveDao,
    private val subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
) : AutomatiseringStansetSjekker {
    private val subsumsjonsmelder by lazy { subsumsjonsmelderProvider() }

    object Factory {
        fun stansAutomatiskBehandlingMediator(
            sessionContext: SessionContext,
            subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
        ): StansAutomatiskBehandlingMediator =
            StansAutomatiskBehandlingMediator(
                sessionContext.stansAutomatiskBehandlingDao,
                sessionContext.periodehistorikkDao,
                sessionContext.oppgaveDao,
                subsumsjonsmelderProvider,
            )
    }

    fun håndter(melding: StansAutomatiskBehandlingMelding) {
        stansAutomatiskBehandlingDao.lagreFraISyfo(melding)
        lagrePeriodehistorikk(melding.fødselsnummer())
    }

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
        val oppgaveId = oppgaveDao.finnOppgaveId(fødselsnummer)
        if (oppgaveId != null) {
            val innslag = Historikkinnslag.automatiskBehandlingStanset()
            periodehistorikkDao.lagreMedOppgaveId(innslag, oppgaveId)
        } else {
            sikkerlogg.info("Fant ikke oppgave for $fødselsnummer. Fikk ikke lagret historikkinnslag om stans av automatisk behandling")
        }
    }

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

    private fun sendSubsumsjonMeldinger(
        stoppmeldinger: List<StoppknappmeldingForSubsumsjon>,
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
    ) = stoppmeldinger
        .byggSubsumsjonEventer(fødselsnummer, vedtaksperiodeId, organisasjonsnummer)
        .toMutableList()
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
