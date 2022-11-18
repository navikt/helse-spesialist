package no.nav.helse.modell.kommando

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.HendelseDao
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.objectMapper
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class UtbetalingsgodkjenningCommandTest {
    private companion object {
        private const val GODKJENT = true
        private const val IDENT = "Z999999"
        private const val EPOST = "saksbehandler@nav.no"
        private val OID = UUID.randomUUID()
        private val TIDSPUNKT = LocalDateTime.now()
        private val GODKJENNINGSBEHOV_ID = UUID.randomUUID()
        private val vedtaksperiodeId = UUID.randomUUID()
        private val fødselsnummer = "1234"
    }

    private val behov = UtbetalingsgodkjenningMessage("""{ "@event_name": "behov" }""")
    private val dao = mockk<HendelseDao>()
    private val varselRepository = mockk<VarselRepository>(relaxed = true)
    private lateinit var commandContext: CommandContext
    private lateinit var command: UtbetalingsgodkjenningCommand

    @BeforeEach
    fun setup() {
        clearMocks(dao)
        commandContext = CommandContext(UUID.randomUUID())
        command = UtbetalingsgodkjenningCommand(
            godkjent = GODKJENT,
            saksbehandlerIdent = IDENT,
            oid = OID,
            epostadresse = EPOST,
            godkjenttidspunkt = TIDSPUNKT,
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            godkjenningsbehovhendelseId = GODKJENNINGSBEHOV_ID,
            hendelseDao = dao,
            godkjenningMediator = GodkjenningMediator(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), varselRepository),
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer
        )
    }

    @Test
    fun `løser godkjenningsbehovet`() {
        every { dao.finnUtbetalingsgodkjenningbehov(GODKJENNINGSBEHOV_ID) } returns behov
        assertTrue(command.execute(commandContext))
        assertNotNull(commandContext.meldinger()
            .map(objectMapper::readTree)
            .filter { it["@event_name"].asText() == "behov" }
            .firstOrNull { it["@løsning"].hasNonNull("Godkjenning") })
    }
}
