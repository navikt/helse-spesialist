package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.MinimumSykdomsgradVurdertEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode.Companion.byggSubsumsjoner
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_IKKE_OPPFYLT
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_OPPFYLT
import java.time.LocalDate
import java.util.UUID

class MinimumSykdomsgrad(
    override val id: UUID = UUID.randomUUID(),
    override val saksbehandler: Saksbehandler,
    val aktørId: String,
    override val fødselsnummer: String,
    val perioderVurdertOk: List<MinimumSykdomsgradPeriode>,
    val perioderVurdertIkkeOk: List<MinimumSykdomsgradPeriode>,
    val begrunnelse: String,
    val arbeidsgivere: List<MinimumSykdomsgradArbeidsgiver>,
    val initierendeVedtaksperiodeId: UUID,
) : Overstyring {
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
        return MinimumSykdomsgradVurdertEvent(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            saksbehandlerOid = oid,
            saksbehandlerNavn = navn,
            saksbehandlerIdent = ident,
            saksbehandlerEpost = epost,
            perioderMedMinimumSykdomsgradVurdertOk = perioderVurdertOk,
            perioderMedMinimumSykdomsgradVurdertIkkeOk = perioderVurdertIkkeOk,
        )
    }

    internal fun byggSubsumsjoner(saksbehandlerEpost: String): List<Subsumsjon> {
        return perioderVurdertOk.byggSubsumsjoner(
            overstyringId = id,
            fødselsnummer = fødselsnummer,
            saksbehandlerEpost = saksbehandlerEpost,
            initierendeVedtaksperiodeId = initierendeVedtaksperiodeId,
            arbeidsgivere = arbeidsgivere,
            avslag = false,
        ).plus(
            perioderVurdertIkkeOk.byggSubsumsjoner(
                overstyringId = id,
                fødselsnummer = fødselsnummer,
                saksbehandlerEpost = saksbehandlerEpost,
                initierendeVedtaksperiodeId = initierendeVedtaksperiodeId,
                arbeidsgivere = arbeidsgivere,
                avslag = true,
            ),
        )
    }
}

class MinimumSykdomsgradArbeidsgiver(
    val organisasjonsnummer: String,
    val berørtVedtaksperiodeId: UUID,
)

class MinimumSykdomsgradPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    internal companion object {
        internal fun List<MinimumSykdomsgradPeriode>.byggSubsumsjoner(
            overstyringId: UUID,
            fødselsnummer: String,
            saksbehandlerEpost: String,
            initierendeVedtaksperiodeId: UUID,
            arbeidsgivere: List<MinimumSykdomsgradArbeidsgiver>,
            avslag: Boolean,
        ): List<Subsumsjon> =
            this.map { periode ->
                Subsumsjon(
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
                            "fom" to periode.fom,
                            "tom" to periode.tom,
                            "initierendeVedtaksperiode" to initierendeVedtaksperiodeId,
                        ),
                    output = emptyMap(),
                    utfall = if (!avslag) VILKAR_OPPFYLT else VILKAR_IKKE_OPPFYLT,
                    sporing =
                        Subsumsjon.SporingVurdertMinimumSykdomsgrad(
                            vedtaksperioder = arbeidsgivere.map { it.berørtVedtaksperiodeId },
                            organisasjonsnummer = arbeidsgivere.map { it.organisasjonsnummer },
                            minimumSykdomsgradId = overstyringId,
                            saksbehandler = listOf(saksbehandlerEpost),
                        ),
                )
            }
    }
}
