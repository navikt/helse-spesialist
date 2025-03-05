package no.nav.helse.modell.saksbehandler.handlinger

import no.nav.helse.modell.melding.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver.Companion.byggSubsumsjon
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_BEREGNET
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
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
    override var ferdigstilt: Boolean,
    kobledeVedtaksperioder: List<UUID>,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
) : Overstyring(id) {
    override val kobledeVedtaksperioder = kobledeVedtaksperioder.toMutableList()

    override fun utførAv(legacySaksbehandler: LegacySaksbehandler) {
        legacySaksbehandler.håndter(this)
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
            arbeidsgivere = arbeidsgivere,
            kobledeVedtaksperioder = emptyList(),
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
            kobledeVedtaksperioder: List<UUID>,
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
            arbeidsgivere = arbeidsgivere,
            kobledeVedtaksperioder = kobledeVedtaksperioder,
        )
    }

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

data class SkjønnsfastsattArbeidsgiver(
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
