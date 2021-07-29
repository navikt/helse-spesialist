package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class GodkjenningsbehovRiverTest {
    private companion object {
        private val HENDELSE = UUID.randomUUID()
        private val VEDTAKSPERIODE = UUID.randomUUID()
        private val UTBETALING_ID = UUID.randomUUID()
        private const val FNR = "12345678911"
        private const val AKTØR = "1234567891234"
        private const val ORGNR = "123456789"
        private val FOM = LocalDate.of(2020, 1, 1)
        private val TOM = LocalDate.of(2020, 1, 31)
    }
    private val testmeldingfabrikk = Testmeldingfabrikk(FNR, AKTØR)
    private val mediator = mockk<HendelseMediator>(relaxed = true)
    private val testRapid = TestRapid().apply {
        Godkjenningsbehov.GodkjenningsbehovRiver(this, mediator)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `leser Godkjenningbehov`() {
        val aktiveArbeidsforhold = listOf("HALLO⁉")
        testRapid.sendTestMessage(testmeldingfabrikk.lagGodkjenningsbehov(
            id = HENDELSE,
            vedtaksperiodeId = VEDTAKSPERIODE,
            orgnummer = ORGNR,
            skjæringstidspunkt = FOM,
            periodeFom = FOM,
            periodeTom = TOM,
            inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE,
            utbetalingId = UTBETALING_ID,
            orgnummereMedAktiveArbeidsforhold = aktiveArbeidsforhold
        ))
        verify(exactly = 1) { mediator.godkjenningsbehov(
            any(),
            HENDELSE,
            FNR,
            AKTØR,
            ORGNR,
            FOM,
            TOM,
            FOM,
            VEDTAKSPERIODE,
            UTBETALING_ID,
            null,
            Periodetype.FØRSTEGANGSBEHANDLING,
            Utbetalingtype.UTBETALING,
            Inntektskilde.FLERE_ARBEIDSGIVERE,
            listOf(Godkjenningsbehov.AktivVedtaksperiode(ORGNR, VEDTAKSPERIODE, Periodetype.FØRSTEGANGSBEHANDLING)),
            aktiveArbeidsforhold,
            any()
        ) }
    }
}
