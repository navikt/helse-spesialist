package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.ForberedBehandlingAvGodkjenningsbehov
import no.nav.helse.modell.person.LegacyPerson
import no.nav.helse.modell.person.vedtaksperiode.LegacyVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.SpleisVedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.Testdata
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

internal class ForberedBehandlingAvGodkjenningsbehovTest : ApplicationTest() {
    @Test
    fun `skjæringstidspunkt for behandling i VedtakFattet oppdateres ikke ved mottak av nytt godkjenningsbehov`() {
        val spleisBehandlingId1 = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val orgnr = "987654321"

        val behandling1 =
            LegacyBehandling.fraLagring(
                id = UUID.randomUUID(),
                vedtaksperiodeId = vedtaksperiodeId1,
                utbetalingId = UUID.randomUUID(),
                spleisBehandlingId = spleisBehandlingId1,
                skjæringstidspunkt = 1 jan 2018,
                fom = 1 jan 2018,
                tom = 31 jan 2018,
                tilstand = LegacyBehandling.VedtakFattet,
                tags = emptyList(),
                varsler = emptySet(),
                vedtakBegrunnelse = null,
                yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
            )

        val person =
            LegacyPerson(
                aktørId = "1234567890123",
                fødselsnummer = "12345678901",
                vedtaksperioder =
                    listOf(
                        LegacyVedtaksperiode(
                            vedtaksperiodeId = vedtaksperiodeId1,
                            organisasjonsnummer = orgnr,
                            forkastet = false,
                            behandlinger = listOf(behandling1),
                        ),
                    ),
                skjønnsfastsatteSykepengegrunnlag = emptyList(),
                avviksvurderinger = emptyList(),
            )

        val commandData =
            Testdata.godkjenningsbehovData().copy(
                spleisVedtaksperioder =
                    listOf(
                        SpleisVedtaksperiode(
                            vedtaksperiodeId = vedtaksperiodeId1,
                            spleisBehandlingId = spleisBehandlingId1,
                            fom = 1 jan 2018,
                            tom = 31 jan 2018,
                            skjæringstidspunkt = 15 jan 2018,
                        ),
                    ),
            )

        ForberedBehandlingAvGodkjenningsbehov(commandData, person).execute(CommandContext(UUID.randomUUID()), sessionContext, outbox)

        Assertions.assertEquals(1 jan 2018, behandling1.skjæringstidspunkt())
    }
}
