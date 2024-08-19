package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.medRivers
import no.nav.helse.mediator.MeldingMediator
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class GodkjenningsbehovRiverTest {
    private val HENDELSE = UUID.randomUUID()
    private val VEDTAKSPERIODE = UUID.randomUUID()
    private val UTBETALING_ID = UUID.randomUUID()
    private val FNR = "12345678911"
    private val AKTØR = "1234567891234"
    private val ORGNR = "123456789"
    private val FOM = LocalDate.of(2020, 1, 1)
    private val TOM = LocalDate.of(2020, 1, 31)

    private val mediator = mockk<MeldingMediator>(relaxed = true)
    private val testRapid = TestRapid().medRivers(GodkjenningsbehovRiver(mediator))

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser Godkjenningbehov`() {
        val relevanteArbeidsforhold = listOf(ORGNR)
        val vilkårsgrunnlagId = UUID.randomUUID()
        val avviksvurderingId = UUID.randomUUID()
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagGodkjenningsbehov(
                aktørId = AKTØR,
                fødselsnummer = FNR,
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID,
                organisasjonsnummer = ORGNR,
                periodeFom = FOM,
                periodeTom = TOM,
                skjæringstidspunkt = FOM,
                inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE,
                orgnummereMedRelevanteArbeidsforhold = relevanteArbeidsforhold,
                id = HENDELSE,
                kanAvvises = true,
                førstegangsbehandling = true,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                avviksvurderingId = avviksvurderingId,
                periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
                utbetalingtype = Utbetalingtype.UTBETALING
            )
        )
        verify(exactly = 1) { mediator.mottaMelding(
            melding = withArg<Godkjenningsbehov> {
                assertEquals(HENDELSE, it.id)
                assertEquals(FNR, it.fødselsnummer())
                assertEquals(AKTØR, it.aktørId)
                assertEquals(VEDTAKSPERIODE, it.vedtaksperiodeId())
                assertEquals(ORGNR, it.organisasjonsnummer)
                assertEquals(FOM, it.periodeFom)
                assertEquals(TOM, it.periodeTom)
                assertEquals(FOM, it.skjæringstidspunkt)
                assertEquals(Inntektskilde.FLERE_ARBEIDSGIVERE, it.inntektskilde)
                assertEquals(relevanteArbeidsforhold, it.orgnummereMedRelevanteArbeidsforhold)
                assertEquals(true, it.kanAvvises)
                assertEquals(true, it.førstegangsbehandling)
                assertEquals(Periodetype.FØRSTEGANGSBEHANDLING, it.periodetype)
                assertEquals(Utbetalingtype.UTBETALING, it.utbetalingtype)
            },
            messageContext = any()
        ) }
    }

    @Test
    fun `leser ikke Godkjenningbehov uten behandletAvSpinnvill`() {
        val relevanteArbeidsforhold = listOf(ORGNR)
        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagGodkjenningsbehov(
                aktørId = AKTØR,
                fødselsnummer = FNR,
                vedtaksperiodeId = VEDTAKSPERIODE,
                utbetalingId = UTBETALING_ID,
                organisasjonsnummer = ORGNR,
                periodeFom = FOM,
                periodeTom = TOM,
                skjæringstidspunkt = FOM,
                inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE,
                orgnummereMedRelevanteArbeidsforhold = relevanteArbeidsforhold,
                id = HENDELSE,
                avviksvurderingId = null,
            )
        )
        verify(exactly = 0) {
            mediator.mottaMelding(
                melding = any(),
                messageContext = any(),
            )
        }
    }
}
