package no.nav.helse.mediator.api

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import no.nav.helse.MeldingPubliserer
import no.nav.helse.TestRapidHelpers.oppgaveId
import no.nav.helse.TestRapidHelpers.siste
import no.nav.helse.e2e.AbstractE2ETest
import no.nav.helse.kafka.MessageContextMeldingPubliserer
import no.nav.helse.mediator.GodkjenningService
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.spesialist.api.bootstrap.SpeilTilgangsgrupper
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.util.testEnv
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue

// Snakk med Christian før du lager flere subklasser av denne. Det er mulig vi ønsker å lage (eller allerede har laget?)
// et annet opplegg for å teste samspillet mellom API og selve/mediator/modell
internal abstract class AbstractIntegrationTest : AbstractE2ETest() {
    protected val testRapid = __ikke_bruk_denne
    private val meldingPubliserer: MeldingPubliserer = MessageContextMeldingPubliserer(testRapid)
    protected val oppgaveDao = daos.oppgaveDao
    private val reservasjonDao = daos.reservasjonDao
    private val periodehistorikkDao = daos.periodehistorikkDao
    private val saksbehandlerDao = daos.saksbehandlerDao

    private val oppgaveService =
        OppgaveService(
            oppgaveDao = oppgaveDao,
            tildelingDao = daos.tildelingDao,
            reservasjonDao = reservasjonDao,
            opptegnelseDao = daos.opptegnelseDao,
            meldingPubliserer = meldingPubliserer,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
            tilgangsgrupper = SpeilTilgangsgrupper(testEnv),
            repositories = daos
        )

    val godkjenningService =
        GodkjenningService(
            oppgaveDao = oppgaveDao,
            overstyringDao = daos.overstyringDao,
            publiserer = MessageContextMeldingPubliserer(testRapid),
            oppgaveService = oppgaveService,
            reservasjonDao = reservasjonDao,
            periodehistorikkDao = periodehistorikkDao,
            saksbehandlerDao = saksbehandlerDao,
            sessionFactory = sessionFactory,
        )

    protected fun sisteOppgaveId() = testRapid.inspektør.oppgaveId()

    protected fun assertSaksbehandlerløsning(
        godkjent: Boolean,
        automatiskBehandlet: Boolean,
        totrinnsvurdering: Boolean,
        vararg årsakerTilAvvist: String,
    ) {
        val løsning = testRapid.inspektør.siste("saksbehandler_løsning")
        assertTrue(løsning.path("godkjent").isBoolean)
        assertEquals(godkjent, løsning.path("godkjent").booleanValue())
        assertEquals(automatiskBehandlet, løsning.path("automatiskBehandling").booleanValue())
        assertNotNull(løsning.path("fødselsnummer").asText())
        assertNotNull(løsning.get("oppgaveId").asLong())
        assertNotNull(løsning.get("hendelseId").asText())
        assertNotNull(løsning.get("saksbehandlerident").asText())
        assertNotNull(løsning.path("saksbehandleroid").asText())
        assertNotNull(løsning.path("saksbehandlerepost").asText())
        assertNotNull(løsning.path("godkjenttidspunkt").asLocalDateTime())
        assertNotNull(løsning.path("saksbehandler"))
        if (årsakerTilAvvist.isNotEmpty()) {
            val begrunnelser = løsning["begrunnelser"].map { it.asText() }
            assertEquals(begrunnelser, begrunnelser.distinct())
            assertEquals(årsakerTilAvvist.toSet(), begrunnelser.toSet())
        }

        if (totrinnsvurdering) {
            assertNotNull(løsning["beslutter"])
        } else {
            assertNull(løsning["beslutter"])
        }
    }
}
