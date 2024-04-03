package no.nav.helse.modell.saksbehandler.handlinger.dto

import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.vilkårsprøving.LovhjemmelDto
import java.time.LocalDate
import java.util.UUID

data class SkjønnsfastsattSykepengegrunnlagDto(
    val id: UUID,
    val aktørId: String,
    val fødselsnummer: String,
    val skjæringstidspunkt: LocalDate,
    val arbeidsgivere: List<SkjønnsfastsattArbeidsgiverDto>,
)

data class SkjønnsfastsattArbeidsgiverDto(
    val organisasjonsnummer: String,
    val årlig: Double,
    val fraÅrlig: Double,
    val årsak: String,
    val type: SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype,
    val begrunnelseMal: String?,
    val begrunnelseFritekst: String?,
    val begrunnelseKonklusjon: String?,
    val lovhjemmel: LovhjemmelDto?,
    val initierendeVedtaksperiodeId: String?,
)
