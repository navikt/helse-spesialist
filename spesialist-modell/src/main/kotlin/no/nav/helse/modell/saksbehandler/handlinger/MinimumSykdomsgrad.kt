package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.saksbehandler.MinimumSykdomsgradVurdertEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.dto.MinimumSykdomsgradDto
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_OPPFYLT
import java.time.LocalDate
import java.util.UUID

class MinimumSykdomsgrad(
    private val id: UUID = UUID.randomUUID(),
    private val aktørId: String,
    private val fødselsnummer: String,
    private val fom: LocalDate,
    private val tom: LocalDate,
    private val vurdering: Boolean,
    private val begrunnelse: String,
    private val arbeidsgivere: List<MinimumSykdomsgradArbeidsgiver>,
    private val initierendeVedtaksperiodeId: UUID,
) : Overstyring {
    override fun gjelderFødselsnummer(): String = fødselsnummer

    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "minimum_sykdomsgrad_vurdert"

    fun byggEvent(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ): MinimumSykdomsgradVurdertEvent {
        val vurdertOk = if (vurdering) listOf(Vurdering(fom, tom)) else emptyList()
        val vurdertIkkeOk = if (!vurdering) listOf(Vurdering(fom, tom)) else emptyList()

        return MinimumSykdomsgradVurdertEvent(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            saksbehandlerOid = oid,
            saksbehandlerNavn = navn,
            saksbehandlerIdent = ident,
            saksbehandlerEpost = epost,
            perioderMedMinimumSykdomsgradVurdertOk = vurdertOk,
            perioderMedMinimumSykdomsgradVurdertIkkeOk = vurdertIkkeOk,
        )
    }

    fun toDto() =
        MinimumSykdomsgradDto(
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            fom = fom,
            tom = tom,
            vurdering = vurdering,
            begrunnelse = begrunnelse,
            arbeidsgivere = arbeidsgivere.map(MinimumSykdomsgradArbeidsgiver::toDto),
            initierendeVedtaksperiodeId = initierendeVedtaksperiodeId,
        )

    internal fun byggSubsumsjon(saksbehandlerEpost: String): Subsumsjon {
        return Subsumsjon(
            lovhjemmel =
                Lovhjemmel(
                    paragraf = "8-13",
                    ledd = "1",
                    lovverk = "folketrygdloven",
                    lovverksversjon = "2019-01-01",
                ),
            fødselsnummer = fødselsnummer,
            input =
                mapOf(
                    "fom" to fom,
                    "tom" to tom,
                    "initierendeVedtaksperiode" to initierendeVedtaksperiodeId,
                ),
            output = emptyMap(),
            utfall = if (vurdering) VILKAR_OPPFYLT else VILKAR_IKKE_OPPFYLT,
            sporing =
                SporingSkjønnsfastsattSykepengegrunnlag(
                    vedtaksperioder = arbeidsgivere.map { it.toDto().berørtVedtaksperiodeId },
                    organisasjonsnummer = arbeidsgivere.map { it.toDto().organisasjonsnummer },
                    saksbehandler = listOf(saksbehandlerEpost),
                ),
        )
    }
}

class MinimumSykdomsgradArbeidsgiver(
    private val organisasjonsnummer: String,
    private val berørtVedtaksperiodeId: UUID,
) {
    fun toDto() =
        MinimumSykdomsgradDto.MinimumSykdomsgradArbeidsgiverDto(
            organisasjonsnummer = organisasjonsnummer,
            berørtVedtaksperiodeId = berørtVedtaksperiodeId,
        )
}

data class Vurdering(
    val fom: LocalDate,
    val tom: LocalDate,
)
