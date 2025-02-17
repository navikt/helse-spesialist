package no.nav.helse.spesialist.application

import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektsopplysningkilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.SpleisSykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.SykepengegrunnlagsArbeidsgiver
import java.util.UUID

object Testdata {
    fun godkjenningsbehovData(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String = lagFødselsnummer(),
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        spleisBehandlingId: UUID = UUID.randomUUID(),
        tags: List<String> = emptyList(),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        utbetalingtype: no.nav.helse.modell.utbetaling.Utbetalingtype = UTBETALING,
        kanAvvises: Boolean = true,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        inntektsopplysningkilde: Inntektsopplysningkilde = Inntektsopplysningkilde.Arbeidsgiver,
        json: String = "{}",
    ): GodkjenningsbehovData {
        return GodkjenningsbehovData(
            id = id,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisVedtaksperioder = emptyList(),
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            avviksvurderingId = UUID.randomUUID(),
            vilkårsgrunnlagId = UUID.randomUUID(),
            tags = tags,
            periodeFom = 1 jan 2018,
            periodeTom = 31 jan 2018,
            periodetype = periodetype,
            førstegangsbehandling = true,
            utbetalingtype = utbetalingtype,
            kanAvvises = kanAvvises,
            inntektskilde = inntektskilde,
            orgnummereMedRelevanteArbeidsforhold = emptyList(),
            skjæringstidspunkt = 1 jan 2018,
            spleisSykepengegrunnlagsfakta = SpleisSykepengegrunnlagsfakta(
                arbeidsgivere = listOf(
                    SykepengegrunnlagsArbeidsgiver(
                        arbeidsgiver = organisasjonsnummer,
                        omregnetÅrsinntekt = 123456.7,
                        inntektskilde = inntektsopplysningkilde,
                        skjønnsfastsatt = null
                    )
                )
            ),
            json = json,
        )
    }

}
