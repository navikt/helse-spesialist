package no.nav.helse.spesialist.e2etests.tests

import no.nav.helse.spesialist.e2etests.AbstractE2EIntegrationTest
import kotlin.test.Test

class AutomatiskTildelVedReservasjonE2ETest: AbstractE2EIntegrationTest() {
    @Test
    fun `tildel oppgave automatisk til saksbehandler etter reberegning`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
        }

        spleisReberegnerAutomatisk(førsteVedtaksperiode())
        spleisSenderGodkjenningsbehov(førsteVedtaksperiode())

        assertOppgaveTildeltSaksbehandler()
    }
    @Test
    fun `tildel oppgave automatisk til saksbehandler etter overstyring`() {
        risikovurderingBehovLøser.kanGodkjenneAutomatisk = false
        søknadOgGodkjenningbehovKommerInn()
        medPersonISpeil {
            saksbehandlerTildelerSegSaken()
            saksbehandlerSkjønnsfastsetter830TredjeAvsnitt()
        }

        assertOppgaveTildeltSaksbehandler()
    }
}
