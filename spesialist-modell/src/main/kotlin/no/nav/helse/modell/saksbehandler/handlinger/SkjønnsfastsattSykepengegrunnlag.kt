package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver.Companion.byggSubsumsjon
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_BEREGNET
import java.time.LocalDate
import java.util.UUID

class SkjønnsfastsattSykepengegrunnlag(
    override val eksternHendelseId: UUID = UUID.randomUUID(),
    override val saksbehandler: Saksbehandler,
    override val fødselsnummer: String,
    val aktørId: String,
    val vedtaksperiodeId: UUID,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
) : Overstyring {
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }

    override fun loggnavn(): String = "skjønnsfastsett_sykepengegrunnlag"

    fun byggEvent(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ): SkjønnsfastsattSykepengegrunnlagEvent {
        return SkjønnsfastsattSykepengegrunnlagEvent(
            eksternHendelseId = eksternHendelseId,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            saksbehandlerOid = oid,
            saksbehandlerNavn = navn,
            saksbehandlerIdent = ident,
            saksbehandlerEpost = epost,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere.map(SkjønnsfastsattArbeidsgiver::byggEvent),
        )
    }

    internal fun byggSubsumsjon(saksbehandlerEpost: String): Subsumsjon {
        return arbeidsgivere.byggSubsumsjon(saksbehandlerEpost, fødselsnummer)
    }
}

class SkjønnsfastsattArbeidsgiver(
    val organisasjonsnummer: String,
    val årlig: Double,
    val fraÅrlig: Double,
    val årsak: String,
    val type: Skjønnsfastsettingstype,
    val begrunnelseMal: String?,
    val begrunnelseFritekst: String?,
    val begrunnelseKonklusjon: String?,
    val lovhjemmel: Lovhjemmel?,
    val initierendeVedtaksperiodeId: String?,
) {
    internal companion object {
        internal fun List<SkjønnsfastsattArbeidsgiver>.byggSubsumsjon(
            saksbehandlerEpost: String,
            fødselsnummer: String,
        ) = Subsumsjon(
            lovhjemmel = this.first().lovhjemmel!!,
            fødselsnummer = fødselsnummer,
            input =
                mapOf(
                    "sattÅrligInntektPerArbeidsgiver" to this.map { ag -> mapOf(ag.organisasjonsnummer to ag.årlig) },
                ),
            output =
                mapOf(
                    "grunnlagForSykepengegrunnlag" to this.sumOf { ag -> ag.årlig },
                ),
            utfall = VILKAR_BEREGNET,
            sporing =
                SporingSkjønnsfastsattSykepengegrunnlag(
                    vedtaksperioder = this.mapNotNull { ag -> ag.initierendeVedtaksperiodeId?.let { UUID.fromString(it) } },
                    organisasjonsnummer = this.map { ag -> ag.organisasjonsnummer },
                    saksbehandler = listOf(saksbehandlerEpost),
                ),
        )
    }

    fun byggEvent() =
        SkjønnsfastsattSykepengegrunnlagEvent.SkjønnsfastsattArbeidsgiver(
            organisasjonsnummer = organisasjonsnummer,
            årlig = årlig,
            fraÅrlig = fraÅrlig,
            årsak = årsak,
            type =
                when (type) {
                    Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT -> "OMREGNET_ÅRSINNTEKT"
                    Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT -> "RAPPORTERT_ÅRSINNTEKT"
                    Skjønnsfastsettingstype.ANNET -> "ANNET"
                },
            begrunnelseMal = begrunnelseMal,
            begrunnelseFritekst = begrunnelseFritekst,
            begrunnelseKonklusjon = begrunnelseKonklusjon,
            initierendeVedtaksperiodeId = initierendeVedtaksperiodeId,
        )

    enum class Skjønnsfastsettingstype {
        OMREGNET_ÅRSINNTEKT,
        RAPPORTERT_ÅRSINNTEKT,
        ANNET,
    }
}
