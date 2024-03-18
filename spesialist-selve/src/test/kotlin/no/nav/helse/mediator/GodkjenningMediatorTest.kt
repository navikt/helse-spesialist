package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.januar
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.IVedtaksperiodeObserver
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GodkjenningMediatorTest {
    private lateinit var context: CommandContext
    private val opptegnelseDao = mockk<OpptegnelseDao>(relaxed = true)
    private val observer = object : IVedtaksperiodeObserver {
        val generasjonerMedGodkjenteVarsler = mutableSetOf<UUID>()
        override fun varselOpprettet(
            varselId: UUID,
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselkode: String,
            opprettet: LocalDateTime
        ) {
            generasjonerMedGodkjenteVarsler.add(generasjonId)
        }
    }
    private val hendelserInspektør = object: UtgåendeMeldingerObserver {
        private val hendelser = mutableListOf<JsonNode>()

        fun hendelser(eventName: String) = hendelser.filter { it["@event_name"]?.asText() == eventName }

        override fun hendelse(hendelse: String) {
            hendelser.add(objectMapper.readTree(hendelse))
        }

        override fun behov(behov: String, ekstraKontekst: Map<String, Any>, detaljer: Map<String, Any>) {}
    }
    private val mediator = GodkjenningMediator(
        vedtakDao = mockk(relaxed = true),
        opptegnelseDao = opptegnelseDao,
        oppgaveDao = mockk(relaxed = true),
        utbetalingDao = mockk(relaxed = true),
        hendelseDao = mockk(relaxed = true) {
            every { finnUtbetalingsgodkjenningbehovJson(any()) } returns "{}"
            every { finnFødselsnummer(any()) } returns fnr
        },
    )

    private val saksbehandler = Saksbehandlerløsning.Saksbehandler(
        ident = "saksbehandlerident",
        epostadresse = "saksbehandler@nav.no"
    )

    private val beslutter = Saksbehandlerløsning.Saksbehandler(
        ident = "beslutterident",
        epostadresse = "beslutter@nav.no"
    )

    private val utbetaling = Utbetaling(UUID.randomUUID(), 1000, 1000, Utbetalingtype.UTBETALING)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(hendelserInspektør)
        clearMocks(opptegnelseDao)
    }

    @Test
    fun `automatisk avvisning skal opprette opptegnelse`() {
        mediator.automatiskAvvisning(context::publiser, UUID.randomUUID(), listOf("foo"), utbetaling, UUID.randomUUID())
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    @Test
    fun `automatisk utbetaling skal opprette opptegnelse`() {
        mediator.automatiskUtbetaling(context, UtbetalingsgodkjenningMessage("{}", utbetaling), UUID.randomUUID(), fnr, UUID.randomUUID())
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    @Test
    fun `saksbehandler utbetaling skal ikke opprette opptegnelse`() {
        mediator.saksbehandlerUtbetaling(
            behandlingId = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            saksbehandlerIdent = "1",
            saksbehandlerEpost = "2@nav.no",
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1.januar, listOf(generasjon()), emptyList())
        )
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `legg saksbehandler og beslutter på vedtaksperiode_godkjent`() {
        mediator.saksbehandlerUtbetaling(
            behandlingId = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            saksbehandlerIdent = "1",
            saksbehandlerEpost = "2@nav.no",
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1.januar, listOf(generasjon()), emptyList())
        )
        val hendelser = hendelserInspektør.hendelser("vedtaksperiode_godkjent")
        assertEquals(1, hendelser.size)
        val vedtaksperiodeGodkjent = hendelser.single()
        assertEquals(saksbehandler.ident, vedtaksperiodeGodkjent["saksbehandler"]["ident"].asText())
        assertEquals(saksbehandler.epostadresse, vedtaksperiodeGodkjent["saksbehandler"]["epostadresse"].asText())
        assertEquals(beslutter.ident, vedtaksperiodeGodkjent["beslutter"]["ident"].asText())
        assertEquals(beslutter.epostadresse, vedtaksperiodeGodkjent["beslutter"]["epostadresse"].asText())
        assertEquals(false, vedtaksperiodeGodkjent["automatiskBehandling"].asBoolean())
    }

    @Test
    fun `ikke legg beslutter på vedtaksperiode_godkjent dersom den ikke er satt på meldingen inn`() {
        mediator.saksbehandlerUtbetaling(
            behandlingId = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            saksbehandlerIdent = "1",
            saksbehandlerEpost = "2@nav.no",
            saksbehandler = saksbehandler,
            beslutter = null,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1.januar, listOf(generasjon()), emptyList())
        )
        val hendelser = hendelserInspektør.hendelser("vedtaksperiode_godkjent")
        val vedtaksperiodeGodkjent = hendelser.single()
        assertEquals(null, vedtaksperiodeGodkjent["beslutter"]?.get("ident")?.asText())
        assertEquals(null, vedtaksperiodeGodkjent["beslutter"]?.get("epostadresse")?.asText())
        assertEquals(false, vedtaksperiodeGodkjent["automatiskBehandling"].asBoolean())
    }

    @Test
    fun `legg på saksbehandler men ikke beslutter på vedtaksperiode_avvist`() {
        mediator.saksbehandlerAvvisning(
            behandlingId = UUID.randomUUID(),
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            saksbehandlerIdent = "1",
            saksbehandlerEpost = "2@nav.no",
            saksbehandler = saksbehandler,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            kommentar = null,
            årsak = null,
            begrunnelser = emptyList()
        )
        val hendelser = hendelserInspektør.hendelser("vedtaksperiode_avvist")
        val vedtaksperiodeGodkjent = hendelser.single()
        assertEquals(saksbehandler.ident, vedtaksperiodeGodkjent["saksbehandler"]["ident"].asText())
        assertEquals(saksbehandler.epostadresse, vedtaksperiodeGodkjent["saksbehandler"]["epostadresse"].asText())
        assertEquals(null, vedtaksperiodeGodkjent["beslutter"]?.get("ident")?.asText())
        assertEquals(null, vedtaksperiodeGodkjent["beslutter"]?.get("epostadresse")?.asText())
        assertEquals(false, vedtaksperiodeGodkjent["automatiskBehandling"].asBoolean())
    }

    @Test
    fun `legg på tbd-saksbehandler på vedtaksperiode_godkjent ved automatisk utbetaling`() {
        mediator.automatiskUtbetaling(
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            hendelseId = UUID.randomUUID()
        )
        val hendelser = hendelserInspektør.hendelser("vedtaksperiode_godkjent")
        val vedtaksperiodeGodkjent = hendelser.single()
        assertEquals("Automatisk behandlet", vedtaksperiodeGodkjent["saksbehandler"]["ident"].asText())
        assertEquals("tbd@nav.no", vedtaksperiodeGodkjent["saksbehandler"]["epostadresse"].asText())
        assertEquals(true, vedtaksperiodeGodkjent["automatiskBehandling"].asBoolean())
    }

    @Test
    fun `legg på tbd-saksbehandler på vedtaksperiode_avvist ved automatisk avvisning`() {
        mediator.automatiskAvvisning(
            publiserer = context::publiser,
            vedtaksperiodeId = UUID.randomUUID(),
            hendelseId = UUID.randomUUID(),
            begrunnelser = emptyList(),
            utbetaling = utbetaling
        )
        val hendelser = hendelserInspektør.hendelser("vedtaksperiode_avvist")
        val vedtaksperiodeGodkjent = hendelser.single()
        assertEquals("Automatisk behandlet", vedtaksperiodeGodkjent["saksbehandler"]["ident"].asText())
        assertEquals("tbd@nav.no", vedtaksperiodeGodkjent["saksbehandler"]["epostadresse"].asText())
        assertEquals(true, vedtaksperiodeGodkjent["automatiskBehandling"].asBoolean())
    }

    @Test
    fun `saksbehandler avvisning skal ikke opprette opptegnelse`() {
        mediator.saksbehandlerAvvisning(
            behandlingId = UUID.randomUUID(),
            context,
            UtbetalingsgodkjenningMessage("{}", utbetaling),
            UUID.randomUUID(),
            fnr,
            "1",
            "2@nav.no",
            saksbehandler,
            LocalDateTime.now(),
            null,
            null,
            null,
            emptyList()
        )
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `godkjenner varsler for alle gjeldende generasjoner`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId1, vedtaksperiodeId1)
        val generasjon2 = generasjon(generasjonId2, vedtaksperiodeId2)
        generasjon1.registrer(observer)
        generasjon2.registrer(observer)
        val varsel1 = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId1)
        val varsel2 = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId2)
        generasjon1.håndterNyttVarsel(varsel1, UUID.randomUUID())
        generasjon2.håndterNyttVarsel(varsel2, UUID.randomUUID())

        godkjenning(listOf(generasjon1, generasjon2))
        assertEquals(2, observer.generasjonerMedGodkjenteVarsler.size)
        assertEquals(generasjonId1, observer.generasjonerMedGodkjenteVarsler.toList()[0])
        assertEquals(generasjonId2, observer.generasjonerMedGodkjenteVarsler.toList()[1])
    }

    private fun generasjon(id: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()) = Generasjon(
        id = id,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar
    )

    private fun godkjenning(generasjoner: List<Generasjon>) = mediator.saksbehandlerUtbetaling(
        behandlingId = UUID.randomUUID(),
        UUID.randomUUID(),
        context,
        UtbetalingsgodkjenningMessage("{}", utbetaling),
        UUID.randomUUID(),
        fnr,
        "Z000000",
        "saksbehandler@nav.no",
        saksbehandler,
        beslutter,
        LocalDateTime.now(),
        emptyList(),
        Sykefraværstilfelle(fnr, 1.januar, generasjoner, emptyList())
    )

    private fun assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet() = verify(exactly = 1) {
        opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseType.FERDIGBEHANDLET_GODKJENNINGSBEHOV))
    }

    private fun assertOpptegnelseIkkeOpprettet() = verify(exactly = 0) { opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE)) }

    private companion object {
        const val fnr = "12341231221"
    }
}
