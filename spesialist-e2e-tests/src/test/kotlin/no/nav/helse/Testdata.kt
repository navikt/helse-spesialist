package no.nav.helse

import no.nav.helse.modell.utbetaling.Utbetalingtype.UTBETALING
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.application.snapshot.SnapshotAlder
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiver
import no.nav.helse.spesialist.application.snapshot.SnapshotBehandling
import no.nav.helse.spesialist.application.snapshot.SnapshotBeregnetPeriode
import no.nav.helse.spesialist.application.snapshot.SnapshotInntektstype
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodetilstand
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodetype
import no.nav.helse.spesialist.application.snapshot.SnapshotPeriodevilkar
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson
import no.nav.helse.spesialist.application.snapshot.SnapshotSykepengedager
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetaling
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingstatus
import no.nav.helse.spesialist.application.snapshot.SnapshotUtbetalingtype
import no.nav.helse.spesialist.test.TestPerson
import no.nav.helse.util.desember
import no.nav.helse.util.januar
import java.time.LocalDate
import java.util.UUID

object Testdata {
    private val testperson = TestPerson()

    fun snapshot(
        versjon: Int = 1,
        aktørId: String = testperson.aktørId,
        organisasjonsnummer: String = testperson.orgnummer,
        fødselsnummer: String,
        vedtaksperiodeId: UUID = testperson.vedtaksperiodeId1,
        utbetalingId: UUID = testperson.utbetalingId1,
        arbeidsgiverbeløp: Int = 30000,
        personbeløp: Int = 0,
        utbetaling: SnapshotUtbetaling =
            SnapshotUtbetaling(
                id = utbetalingId,
                arbeidsgiverFagsystemId = "EN_FAGSYSTEMID",
                arbeidsgiverNettoBelop = arbeidsgiverbeløp,
                personFagsystemId = "EN_FAGSYSTEMID",
                personNettoBelop = personbeløp,
                statusEnum = SnapshotUtbetalingstatus.UBETALT,
                typeEnum = SnapshotUtbetalingtype.UTBETALING,
                vurdering = null,
                personoppdrag = null,
                arbeidsgiveroppdrag = null,
            ),
    ): SnapshotPerson =
        SnapshotPerson(
            aktorId = aktørId,
            fodselsnummer = fødselsnummer,
            versjon = versjon,
            arbeidsgivere =
                listOf(
                    SnapshotArbeidsgiver(
                        organisasjonsnummer = organisasjonsnummer,
                        ghostPerioder = emptyList(),
                        behandlinger =
                            listOf(
                                SnapshotBehandling(
                                    id = UUID.randomUUID(),
                                    perioder =
                                        listOf(
                                            SnapshotBeregnetPeriode(
                                                behandlingId = UUID.randomUUID(),
                                                vedtaksperiodeId = vedtaksperiodeId,
                                                utbetaling = utbetaling,
                                                erForkastet = false,
                                                fom = 1.januar(2020),
                                                tom = 31.januar(2020),
                                                inntektstype = SnapshotInntektstype.ENARBEIDSGIVER,
                                                opprettet = 31.januar(2020).atStartOfDay(),
                                                periodetype = SnapshotPeriodetype.FORSTEGANGSBEHANDLING,
                                                tidslinje = emptyList(),
                                                forbrukteSykedager = null,
                                                gjenstaendeSykedager = null,
                                                hendelser = emptyList(),
                                                maksdato = 1.januar(2021),
                                                periodevilkar =
                                                    SnapshotPeriodevilkar(
                                                        alder =
                                                            SnapshotAlder(
                                                                alderSisteSykedag = 30,
                                                                oppfylt = true,
                                                            ),
                                                        sykepengedager =
                                                            SnapshotSykepengedager(
                                                                forbrukteSykedager = null,
                                                                gjenstaendeSykedager = null,
                                                                maksdato = 1.januar(2021),
                                                                skjaeringstidspunkt = 1.januar(2020),
                                                                oppfylt = true,
                                                            ),
                                                    ),
                                                skjaeringstidspunkt = 1.januar(2020),
                                                vilkarsgrunnlagId = null,
                                                periodetilstand = SnapshotPeriodetilstand.TILGODKJENNING,
                                                pensjonsgivendeInntekter = emptyList(),
                                                annulleringskandidater = emptyList(),
                                            ),
                                        ),
                                ),
                            ),
                    ),
                ),
            dodsdato = null,
            vilkarsgrunnlag = emptyList(),
        )
}

data class GodkjenningsbehovTestdata(
    val aktørId: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val vedtaksperiodeId: UUID,
    val utbetalingId: UUID,
    val periodeFom: LocalDate = 1.januar,
    val periodeTom: LocalDate = 31.januar,
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
    val foreløpigBeregnetSluttPåSykepenger: LocalDate = 1.desember(),
    val perioderMedSammeSkjæringstidspunkt: List<VedtaksperiodeInfo> = emptyList(),
)

// Det er fritt fram å bytte til et bedre navn på denne :-D
data class VedtaksperiodeInfo(
    val fom: LocalDate,
    val tom: LocalDate,
    val vedtaksperiodeId: UUID,
    val spleisBehandlingId: UUID,
)

data class AvviksvurderingTestdata(
    val avviksprosent: Double = 10.0,
    val sammenligningsgrunnlag: Double = 650_000.0,
    val skjæringstidspunkt: LocalDate = 1.januar,
    val avviksvurderingId: UUID = UUID.randomUUID(),
    val vedtaksperiodeId: UUID = UUID.randomUUID(),
)
