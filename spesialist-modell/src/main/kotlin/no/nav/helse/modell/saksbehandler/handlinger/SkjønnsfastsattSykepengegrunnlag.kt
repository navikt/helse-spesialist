package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.SkjønnsfastsattSykepengegrunnlagEvent
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver.Companion.byggSubsumsjon
import no.nav.helse.modell.saksbehandler.handlinger.dto.SkjønnsfastsattArbeidsgiverDto
import no.nav.helse.modell.saksbehandler.handlinger.dto.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.modell.vilkårsprøving.Subsumsjon
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.SporingSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vilkårsprøving.Subsumsjon.Utfall.VILKAR_BEREGNET

class SkjønnsfastsattSykepengegrunnlag(
    private val id: UUID = UUID.randomUUID(),
    private val aktørId: String,
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
): Overstyring {
    override fun gjelderFødselsnummer(): String = fødselsnummer
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }
    override fun loggnavn(): String = "skjønnsfastsett_sykepengegrunnlag"

    fun byggEvent(oid: UUID, navn: String, epost: String, ident: String): SkjønnsfastsattSykepengegrunnlagEvent {
        return SkjønnsfastsattSykepengegrunnlagEvent(
            id = id,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            saksbehandlerOid = oid,
            saksbehandlerNavn = navn,
            saksbehandlerIdent = ident,
            saksbehandlerEpost = epost,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere.map(SkjønnsfastsattArbeidsgiver::byggEvent)
        )
    }

    fun toDto() = SkjønnsfastsattSykepengegrunnlagDto(
        id = id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        arbeidsgivere = arbeidsgivere.map(SkjønnsfastsattArbeidsgiver::toDto)
    )

    internal fun byggSubsumsjon(saksbehandlerEpost: String): Subsumsjon {
        return arbeidsgivere.byggSubsumsjon(saksbehandlerEpost, fødselsnummer)
    }
}

class SkjønnsfastsattArbeidsgiver(
    private val organisasjonsnummer: String,
    private val årlig: Double,
    private val fraÅrlig: Double,
    private val årsak: String,
    private val type: Skjønnsfastsettingstype,
    private val begrunnelseMal: String?,
    private val begrunnelseFritekst: String?,
    private val begrunnelseKonklusjon: String?,
    private val lovhjemmel: Lovhjemmel?,
    private val initierendeVedtaksperiodeId: String?,
) {

    internal companion object {
        internal fun List<SkjønnsfastsattArbeidsgiver>.byggSubsumsjon(
            saksbehandlerEpost: String,
            fødselsnummer: String,
        ) = Subsumsjon(
            lovhjemmel = this.first().lovhjemmel!!,
            fødselsnummer = fødselsnummer,
            input = mapOf(
                "sattÅrligInntektPerArbeidsgiver" to this.map { ag -> mapOf(ag.organisasjonsnummer to ag.årlig) },
            ),
            output = mapOf(
                "grunnlagForSykepengegrunnlag" to this.sumOf { ag -> ag.årlig }
            ),
            utfall = VILKAR_BEREGNET,
            sporing = SporingSkjønnsfastsattSykepengegrunnlag(
                vedtaksperioder = this.mapNotNull { ag -> UUID.fromString(ag.initierendeVedtaksperiodeId) },
                organisasjonsnummer = this.map { ag -> ag.organisasjonsnummer },
                saksbehandler = listOf(saksbehandlerEpost)
            ),
        )
    }
    fun byggEvent() = SkjønnsfastsattSykepengegrunnlagEvent.SkjønnsfastsattArbeidsgiver(
        organisasjonsnummer = organisasjonsnummer,
        årlig = årlig,
        fraÅrlig = fraÅrlig,
        årsak = årsak,
        type = when (type) {
            Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT -> "OMREGNET_ÅRSINNTEKT"
            Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT -> "RAPPORTERT_ÅRSINNTEKT"
            Skjønnsfastsettingstype.ANNET -> "ANNET"
        },
        begrunnelseMal = begrunnelseMal,
        begrunnelseFritekst = begrunnelseFritekst,
        begrunnelseKonklusjon = begrunnelseKonklusjon,
        initierendeVedtaksperiodeId = initierendeVedtaksperiodeId
    )

    fun toDto() = SkjønnsfastsattArbeidsgiverDto(
        organisasjonsnummer = organisasjonsnummer,
        årlig = årlig,
        fraÅrlig = fraÅrlig,
        årsak = årsak,
        type = type,
        begrunnelseMal = begrunnelseMal,
        begrunnelseFritekst = begrunnelseFritekst,
        begrunnelseKonklusjon = begrunnelseKonklusjon,
        lovhjemmel = lovhjemmel?.toDto(),
        initierendeVedtaksperiodeId = initierendeVedtaksperiodeId,
    )

    enum class Skjønnsfastsettingstype {
        OMREGNET_ÅRSINNTEKT,
        RAPPORTERT_ÅRSINNTEKT,
        ANNET,
    }
}