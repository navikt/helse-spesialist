package no.nav.helse.spesialist.domain.overstyringer

import no.nav.helse.modell.melding.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.modell.saksbehandler.handlinger.Personhandling
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_BEREGNET
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.SaksbehandlerWrapper
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattArbeidsgiver.Companion.byggSubsumsjon
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SkjønnsfastsattSykepengegrunnlag private constructor(
    id: OverstyringId?,
    override val eksternHendelseId: UUID,
    override val saksbehandlerOid: SaksbehandlerOid,
    override val fødselsnummer: String,
    override val aktørId: String,
    override val vedtaksperiodeId: UUID,
    override val opprettet: LocalDateTime,
    ferdigstilt: Boolean,
    val skjæringstidspunkt: LocalDate,
    val årsak: String,
    val type: Skjønnsfastsettingstype,
    val begrunnelseMal: String?,
    val begrunnelseFritekst: String?,
    val begrunnelseKonklusjon: String?,
    val lovhjemmel: Lovhjemmel,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
) : Overstyring(id, ferdigstilt),
    Personhandling {
    override fun utførAv(saksbehandlerWrapper: SaksbehandlerWrapper) {
        saksbehandlerWrapper.håndter(this)
    }

    override fun loggnavn(): String = "skjønnsfastsett_sykepengegrunnlag"

    companion object {
        fun ny(
            saksbehandlerOid: SaksbehandlerOid,
            fødselsnummer: String,
            aktørId: String,
            vedtaksperiodeId: UUID,
            skjæringstidspunkt: LocalDate,
            arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
            årsak: String,
            type: Skjønnsfastsettingstype,
            begrunnelseMal: String?,
            begrunnelseFritekst: String?,
            begrunnelseKonklusjon: String?,
            lovhjemmel: Lovhjemmel,
        ) = SkjønnsfastsattSykepengegrunnlag(
            id = null,
            eksternHendelseId = UUID.randomUUID(),
            opprettet = LocalDateTime.now(),
            ferdigstilt = false,
            saksbehandlerOid = saksbehandlerOid,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            årsak = årsak,
            type = type,
            begrunnelseMal = begrunnelseMal,
            begrunnelseFritekst = begrunnelseFritekst,
            begrunnelseKonklusjon = begrunnelseKonklusjon,
            lovhjemmel = lovhjemmel,
            arbeidsgivere = arbeidsgivere,
        )

        fun fraLagring(
            id: OverstyringId,
            eksternHendelseId: UUID,
            opprettet: LocalDateTime,
            ferdigstilt: Boolean,
            saksbehandlerOid: SaksbehandlerOid,
            fødselsnummer: String,
            aktørId: String,
            vedtaksperiodeId: UUID,
            skjæringstidspunkt: LocalDate,
            arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
            årsak: String,
            type: Skjønnsfastsettingstype,
            begrunnelseMal: String?,
            begrunnelseFritekst: String?,
            begrunnelseKonklusjon: String?,
            lovhjemmel: Lovhjemmel,
        ) = SkjønnsfastsattSykepengegrunnlag(
            id = id,
            eksternHendelseId = eksternHendelseId,
            opprettet = opprettet,
            ferdigstilt = ferdigstilt,
            saksbehandlerOid = saksbehandlerOid,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            vedtaksperiodeId = vedtaksperiodeId,
            skjæringstidspunkt = skjæringstidspunkt,
            årsak = årsak,
            type = type,
            begrunnelseMal = begrunnelseMal,
            begrunnelseFritekst = begrunnelseFritekst,
            begrunnelseKonklusjon = begrunnelseKonklusjon,
            lovhjemmel = lovhjemmel,
            arbeidsgivere = arbeidsgivere,
        )
    }

    fun byggEvent(
        oid: UUID,
        navn: String,
        epost: String,
        ident: String,
    ): SkjønnsfastsattSykepengegrunnlagEvent =
        SkjønnsfastsattSykepengegrunnlagEvent(
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

    fun byggSubsumsjon(saksbehandlerEpost: String): Subsumsjon = arbeidsgivere.byggSubsumsjon(saksbehandlerEpost, fødselsnummer, vedtaksperiodeId, lovhjemmel)
}

data class SkjønnsfastsattArbeidsgiver(
    val organisasjonsnummer: String,
    val årlig: Double,
    val fraÅrlig: Double,
) {
    internal companion object {
        internal fun List<SkjønnsfastsattArbeidsgiver>.byggSubsumsjon(
            saksbehandlerEpost: String,
            fødselsnummer: String,
            vedtaksperiodeId: UUID,
            lovhjemmel: Lovhjemmel,
        ) = Subsumsjon(
            lovhjemmel = lovhjemmel,
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
                    vedtaksperioder = listOf(vedtaksperiodeId),
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
        )

    enum class Skjønnsfastsettingstype {
        OMREGNET_ÅRSINNTEKT,
        RAPPORTERT_ÅRSINNTEKT,
        ANNET,
    }
}
