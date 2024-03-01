package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import no.nav.helse.Testdata.AKTØR
import no.nav.helse.Testdata.FØDSELSNUMMER
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.mediator.HendelseMediator
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfeller
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeOppdatering
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SykefraværstilfellerRiverTest {

    private val testRapid = TestRapid()
    private val mediator = mockk<HendelseMediator>(relaxed = true)

    init {
        SykefraværstilfellerRiver(testRapid, mediator)
    }

    @BeforeEach
    fun setUp() {
        testRapid.reset()
    }

    @Test
    fun `leser inn vedtaksperiode_ny_utbetaling`() {
        val hendelseId = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val vedtaksperiodeId3 = UUID.randomUUID()
        val vedtaksperiodeId4 = UUID.randomUUID()

        val vedtaksperioder1 = listOf(
            mapOf("vedtaksperiodeId" to vedtaksperiodeId1, "organisasjonsnummer" to "zzzzzzzzzzz", "fom" to 1.januar, "tom" to 5.januar),
            mapOf("vedtaksperiodeId" to vedtaksperiodeId2, "organisasjonsnummer" to "zzzzzzzzzzz", "fom" to 6.januar, "tom" to 10.januar),
        )
        val vedtaksperioder2 = listOf(
            mapOf("vedtaksperiodeId" to vedtaksperiodeId3, "organisasjonsnummer" to "zzzzzzzzzzz", "fom" to 1.februar, "tom" to 5.februar),
            mapOf("vedtaksperiodeId" to vedtaksperiodeId4, "organisasjonsnummer" to "zzzzzzzzzzz", "fom" to 6.februar, "tom" to 10.februar),
        )

        testRapid.sendTestMessage(
            Testmeldingfabrikk.lagSykefraværstilfeller(
                fødselsnummer = FØDSELSNUMMER,
                aktørId = AKTØR,
                tilfeller = listOf(
                    mapOf(
                        "dato" to 1.januar,
                        "perioder" to vedtaksperioder1,
                    ),
                    mapOf(
                        "dato" to 1.februar,
                        "perioder" to vedtaksperioder2,
                    )
                ),
                id = hendelseId
            )
        )

        val forventetListeVedtaksperioder = listOf(
            VedtaksperiodeOppdatering(vedtaksperiodeId = vedtaksperiodeId1, skjæringstidspunkt = 1.januar, fom = 1.januar, tom = 5.januar),
            VedtaksperiodeOppdatering(vedtaksperiodeId = vedtaksperiodeId2, skjæringstidspunkt = 1.januar, fom = 6.januar, tom = 10.januar),
            VedtaksperiodeOppdatering(vedtaksperiodeId = vedtaksperiodeId3, skjæringstidspunkt = 1.februar, fom = 1.februar, tom = 5.februar),
            VedtaksperiodeOppdatering(vedtaksperiodeId = vedtaksperiodeId4, skjæringstidspunkt = 1.februar, fom = 6.februar, tom = 10.februar),
        )

        verify(exactly = 1) {
            mediator.håndter(
                melding = withArg<Sykefraværstilfeller> {
                    assertEquals(FØDSELSNUMMER, it.fødselsnummer())
                    assertEquals(AKTØR, it.aktørId)
                    assertEquals(hendelseId, it.id)
                    assertEquals(forventetListeVedtaksperioder, it.vedtaksperiodeOppdateringer)
                },
                messageContext = any()
            )
        }
    }
}
