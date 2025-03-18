package no.nav.helse

import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektsopplysningkilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.SpleisSykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.SykepengegrunnlagsArbeidsgiver
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.spesialist.testfixtures.jan
import no.nav.helse.spesialist.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.testfixtures.lagOrganisasjonsnummer
import java.time.LocalDate
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
                        inntektskilde = Inntektsopplysningkilde.Arbeidsgiver,
                        skjønnsfastsatt = null
                    )
                )
            ),
            erInngangsvilkårVurdertISpleis = true,
            omregnedeÅrsinntekter = listOf(
                OmregnetÅrsinntekt(
                    arbeidsgiverreferanse = organisasjonsnummer,
                    beløp = 123456.7,
                )
            ),
            json = json,
        )
    }
}

internal data class GodkjenningsbehovTestdata(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val periodeFom: LocalDate = 1 jan 2018,
    val periodeTom: LocalDate = 31 jan 2018,
    val skjæringstidspunkt: LocalDate = periodeFom,
    val periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
    val kanAvvises: Boolean = true,
    val førstegangsbehandling: Boolean = true,
    val inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
    val orgnummereMedRelevanteArbeidsforhold: List<String> = emptyList(),
    val utbetalingtype: no.nav.helse.modell.utbetaling.Utbetalingtype = UTBETALING,
    val vilkårsgrunnlagId: UUID = UUID.randomUUID(),
    val spleisBehandlingId: UUID = UUID.randomUUID(),
    val tags: List<String> = emptyList(),
    val spleisSykepengegrunnlagsfakta: SpleisSykepengegrunnlagsfakta = SpleisSykepengegrunnlagsfakta(
        arbeidsgivere = emptyList()
    ),
)
