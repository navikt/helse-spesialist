package no.nav.helse

import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.modell.vedtak.Sykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagFødselsnummer
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import java.time.LocalDate
import java.util.UUID

object Testdata {
    fun godkjenningsbehovData(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String = lagFødselsnummer(),
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
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
            yrkesaktivitetstype = yrkesaktivitetstype,
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
            sykepengegrunnlagsfakta = Sykepengegrunnlagsfakta.Spleis.EtterHovedregel(
                seksG = 6 * 118620.0,
                sykepengegrunnlag = 123456.7,
                arbeidsgivere = listOf(
                    Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                        organisasjonsnummer = organisasjonsnummer,
                        omregnetÅrsinntekt = 123456.7,
                        inntektskilde = Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver
                    )
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
    val yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
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
)
