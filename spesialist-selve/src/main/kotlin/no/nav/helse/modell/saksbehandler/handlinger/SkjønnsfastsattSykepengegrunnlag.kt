package no.nav.helse.modell.saksbehandler.handlinger

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.api.modell.SkjønnsfastsattSykepengegrunnlagEvent

class SkjønnsfastsattSykepengegrunnlag(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val skjæringstidspunkt: LocalDate,
    private val arbeidsgivere: List<SkjønnsfastsattArbeidsgiver>,
): Overstyring {
    override fun gjelderFødselsnummer(): String = fødselsnummer
    override fun utførAv(saksbehandler: Saksbehandler) {
        saksbehandler.håndter(this)
    }
    fun byggEvent(oid: UUID, navn: String, epost: String, ident: String): SkjønnsfastsattSykepengegrunnlagEvent {
        return SkjønnsfastsattSykepengegrunnlagEvent(
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

    class SkjønnsfastsattArbeidsgiver(
        private val organisasjonsnummer: String,
        private val årlig: Double,
        private val fraÅrlig: Double,
        private val årsak: String,
        private val type: Skjønnsfastsettingstype,
        private val begrunnelseMal: String?,
        private val begrunnelseFritekst: String?,
        private val begrunnelseKonklusjon: String?,
        private val subsumsjon: Subsumsjon?,
        private val initierendeVedtaksperiodeId: String?,
    ) {
        fun byggEvent(): SkjønnsfastsattSykepengegrunnlagEvent.SkjønnsfastsattArbeidsgiver {
            return SkjønnsfastsattSykepengegrunnlagEvent.SkjønnsfastsattArbeidsgiver(
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
                subsumsjon = subsumsjon?.byggEvent(),
                initierendeVedtaksperiodeId = initierendeVedtaksperiodeId
            )
        }

        enum class Skjønnsfastsettingstype {
            OMREGNET_ÅRSINNTEKT,
            RAPPORTERT_ÅRSINNTEKT,
            ANNET,
        }
    }
}