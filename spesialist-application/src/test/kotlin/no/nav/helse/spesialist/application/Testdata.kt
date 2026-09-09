package no.nav.helse.spesialist.application

import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.*
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver
import no.nav.helse.spesialist.domain.testfixtures.des
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

object Testdata {
    fun godkjenningsbehovData(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String = lagFødselsnummer(),
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        arbeidssituasjon: Arbeidssituasjon? = null,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        spleisBehandlingId: UUID = UUID.randomUUID(),
        forsikringsvurderingId: UUID? = null,
        tags: List<String> = emptyList(),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        kanAvvises: Boolean = true,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        inntektsopplysningkilde: Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde = Arbeidsgiver,
        json: String = "{}",
        skjæringstidspunkt: LocalDate = 1 jan 2018,
    ): GodkjenningsbehovData =
        GodkjenningsbehovData(
            id = id,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            yrkesaktivitetstype = yrkesaktivitetstype,
            arbeidssituasjon = arbeidssituasjon,
            vedtaksperiodeId = vedtaksperiodeId,
            spleisBehandlinger = emptyList(),
            utbetalingId = utbetalingId,
            spleisBehandlingId = spleisBehandlingId,
            vilkårsgrunnlagId = UUID.randomUUID(),
            forsikringsvurderingId = forsikringsvurderingId,
            tags = tags,
            periodeFom = 1 jan 2018,
            periodeTom = 31 jan 2018,
            periodetype = periodetype,
            førstegangsbehandling = true,
            utbetalingtype = utbetalingtype,
            kanAvvises = kanAvvises,
            inntektskilde = inntektskilde,
            orgnummereMedRelevanteArbeidsforhold = emptyList(),
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlagsfakta =
                Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker.EtterHovedregel(
                    seksG = 6 * 118620.0,
                    arbeidsgivere =
                        listOf(
                            Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                                organisasjonsnummer = organisasjonsnummer,
                                omregnetÅrsinntekt = 123456.7,
                                inntektskilde = inntektsopplysningkilde,
                            ),
                        ),
                    sykepengegrunnlag = BigDecimal("123456.7"),
                ),
            foreløpigBeregnetSluttPåSykepenger = 1 des 2018,
            relevanteSøknader = listOf(UUID.randomUUID()),
            json = json,
        )
}
