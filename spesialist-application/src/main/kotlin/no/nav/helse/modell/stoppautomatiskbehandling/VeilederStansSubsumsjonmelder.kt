package no.nav.helse.modell.stoppautomatiskbehandling

import no.nav.helse.mediator.Subsumsjonsmelder
import no.nav.helse.modell.melding.SubsumsjonEvent
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingStansAutomatiskBehandling
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_OPPFYLT
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_UAVKLART
import no.nav.helse.spesialist.domain.VeilederStans
import java.util.UUID

class VeilederStansSubsumsjonmelder(
    private val subsumsjonsmelderProvider: () -> Subsumsjonsmelder,
) {
    private val subsumsjonsmelder by lazy { subsumsjonsmelderProvider() }

    fun sendMelding(
        veilederStans: VeilederStans?,
        fødselsnummer: String,
        organisasjonsnummer: String,
        vedtaksperiodeId: UUID,
    ) {
        val subsumsjonsEventer =
            veilederStans?.årsaker?.byggSubsumsjonEventer(
                fødselsnummer,
                vedtaksperiodeId,
                organisasjonsnummer,
                veilederStans.originalMeldingId,
            ) ?: emptyList()

        if (subsumsjonsEventer.none { event -> event.paragraf == "8-4" }) {
            subsumsjonsmelder.nySubsumsjon(
                fødselsnummer = fødselsnummer,
                subsumsjonEvent = åtteFireOppfyltEvent(fødselsnummer, vedtaksperiodeId, organisasjonsnummer),
            )
        }

        subsumsjonsEventer.forEach { event ->
            subsumsjonsmelder.nySubsumsjon(fødselsnummer = fødselsnummer, subsumsjonEvent = event)
        }
    }

    private fun Set<VeilederStans.StansÅrsak>.byggSubsumsjonEventer(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
        meldingId: UUID,
    ): List<SubsumsjonEvent> =
        map { årsak ->
            Subsumsjon(
                lovhjemmel = årsak.lovhjemmel(),
                fødselsnummer = fødselsnummer,
                input = mapOf("syfostopp" to true, "årsak" to årsak),
                output = emptyMap(),
                utfall = VILKAR_UAVKLART,
                sporing =
                    SporingStansAutomatiskBehandling(
                        listOf(vedtaksperiodeId),
                        listOf(organisasjonsnummer),
                        listOf(meldingId.toString()),
                    ),
            ).byggEvent()
        }

    private fun åtteFireOppfyltEvent(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        organisasjonsnummer: String,
    ): SubsumsjonEvent =
        Subsumsjon(
            lovhjemmel = ÅTTE_FIRE_FØRSTE_LEDD,
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

    private companion object {
        val ÅTTE_FIRE_FØRSTE_LEDD = Lovhjemmel("8-4", "1", null, "folketrygdloven", "2021-05-21")
        val ÅTTE_ÅTTE_ANDRE_LEDD = Lovhjemmel("8-8", "2", null, "folketrygdloven", "2021-05-21")
        val ÅTTE_ÅTTE_FØRSTE_LEDD = Lovhjemmel("8-8", "1", null, "folketrygdloven", "2021-05-21")

        fun VeilederStans.StansÅrsak.lovhjemmel() =
            when (this) {
                VeilederStans.StansÅrsak.MEDISINSK_VILKAR -> ÅTTE_FIRE_FØRSTE_LEDD
                VeilederStans.StansÅrsak.BESTRIDELSE_SYKMELDING -> ÅTTE_FIRE_FØRSTE_LEDD
                VeilederStans.StansÅrsak.AKTIVITETSKRAV -> ÅTTE_ÅTTE_ANDRE_LEDD
                VeilederStans.StansÅrsak.MANGLENDE_MEDVIRKING -> ÅTTE_ÅTTE_FØRSTE_LEDD
            }
    }
}
