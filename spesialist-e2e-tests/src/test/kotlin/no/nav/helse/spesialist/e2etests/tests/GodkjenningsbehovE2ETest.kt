package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import org.junit.jupiter.api.Test

class GodkjenningsbehovE2ETest : AbstractE2EIntegrationTest() {

    @Test
    fun `kan fortsatt fatte vedtak ved godkjenningsbehov med endret skjæringstidspunkt`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        val vedtaksperiode = førsteVedtaksperiode()
        vedtaksperiode.fom = vedtaksperiode.fom.minusDays(1)
        vedtaksperiode.skjæringstidspunkt = vedtaksperiode.fom

        spleisSenderGodkjenningsbehov(vedtaksperiode)

        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak(vedtaksperiode.spleisBehandlingId!!, "En begrunnelse")
        }

        assertBehandlingTilstand("VedtakFattet")
    }

    @Test
    fun `saksbehandler beholder tildeling ved endret godkjenningsbehov`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()

        medPersonISpeil {
            saksbehandlerTildelerSegSaken() // Må til for å "opprette" saksbehandler
        }

        val vedtaksperiode = førsteVedtaksperiode()
        vedtaksperiode.fom = vedtaksperiode.fom.minusDays(1)
        vedtaksperiode.skjæringstidspunkt = vedtaksperiode.fom

        spleisSenderGodkjenningsbehov(vedtaksperiode)

        medPersonISpeil {
            saksbehandlerGodkjennerAlleVarsler()
            saksbehandlerFatterVedtak(vedtaksperiode.spleisBehandlingId!!, "En begrunnelse")
        }

        assertOppgaveTildeltSaksbehandler()
        assertBehandlingTilstand("VedtakFattet")
    }

}
