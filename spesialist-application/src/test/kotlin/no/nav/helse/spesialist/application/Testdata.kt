package no.nav.helse.spesialist.application

import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Inntektsopplysningkilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.SpleisSykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.SykepengegrunnlagsArbeidsgiver
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.spesialist.api.bootstrap.Gruppe
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.spesialist.testhjelp.jan
import no.nav.helse.spesialist.testhjelp.lagAktørId
import no.nav.helse.spesialist.testhjelp.lagFødselsnummer
import no.nav.helse.spesialist.testhjelp.lagOrganisasjonsnummer
import java.util.UUID

class TestPerson {
    val fødselsnummer: String = lagFødselsnummer()
    val aktørId: String = lagAktørId()
    private val arbeidsgivere = mutableMapOf<Int, TestArbeidsgiver>()
    private val arbeidsgiver1 = nyArbeidsgiver()
    private val arbeidsgiver2 = nyArbeidsgiver()
    private val vedtaksperiode1 = arbeidsgiver1.nyVedtaksperiode()
    private val vedtaksperiode2 = arbeidsgiver1.nyVedtaksperiode()
    val orgnummer: String = arbeidsgiver1.organisasjonsnummer
    private val orgnummer2: String = arbeidsgiver2.organisasjonsnummer
    val vedtaksperiodeId1 = vedtaksperiode1.vedtaksperiodeId
    private val vedtaksperiodeId2 = vedtaksperiode2.vedtaksperiodeId
    private val utbetalingId1 = vedtaksperiode1.utbetalingId
    private val utbetalingId2 = vedtaksperiode2.utbetalingId

    override fun toString(): String {
        return "Testdatasett(fødselsnummer='$fødselsnummer', aktørId='$aktørId', orgnummer='$orgnummer', orgnummer2='$orgnummer2', vedtaksperiodeId1=$vedtaksperiodeId1, vedtaksperiodeId2=$vedtaksperiodeId2, utbetalingId1=$utbetalingId1, utbetalingId2=$utbetalingId2)"
    }

    fun nyArbeidsgiver() = TestArbeidsgiver(fødselsnummer).also {
        arbeidsgivere[arbeidsgivere.size] = it
    }
}

class TestArbeidsgiver(
    val fødselsnummer: String,
) {
    private val vedtaksperioder = mutableMapOf<Int, TestVedtaksperiode>()
    val organisasjonsnummer = lagOrganisasjonsnummer()

    fun nyVedtaksperiode() = TestVedtaksperiode(fødselsnummer, organisasjonsnummer).also {
        vedtaksperioder[vedtaksperioder.size] = it
    }

    val Int.vedtaksperiode
        get() = vedtaksperioder[this] ?: throw IllegalArgumentException(
            "Vedtaksperiode med index $this for arbeidsgiver $organisasjonsnummer finnes ikke",
        )
}

class TestVedtaksperiode(
    val fødselsnummer: String,
    val organisasjonsnummer: String,
) {
    val vedtaksperiodeId: UUID = UUID.randomUUID()
    val utbetalingId: UUID = UUID.randomUUID()
}

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
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
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

val tilgangsgrupper = SpeilTilgangsgrupper(
    env = Gruppe.__indreInnhold_kunForTest().values.associateWith { _ -> UUID.randomUUID().toString() }
)
