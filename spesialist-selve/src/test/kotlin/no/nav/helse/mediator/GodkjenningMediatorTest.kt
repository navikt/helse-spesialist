package no.nav.helse.mediator

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.januar
import no.nav.helse.modell.UtbetalingsgodkjenningMessage
import no.nav.helse.modell.gosysoppgaver.inspektør
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Generasjon
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.abonnement.OpptegnelseDao
import no.nav.helse.spesialist.api.abonnement.OpptegnelseType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class GodkjenningMediatorTest {
    private lateinit var context: CommandContext
    private val opptegnelseDao = mockk<OpptegnelseDao>(relaxed = true)
    private val hendelserInspektør =
        object : CommandContextObserver {
            private val hendelser = mutableListOf<JsonNode>()

            fun hendelser(eventName: String) = hendelser.filter { it["@event_name"]?.asText() == eventName }

            override fun hendelse(hendelse: String) {
                hendelser.add(objectMapper.readTree(hendelse))
            }

            override fun behov(
                behov: String,
                ekstraKontekst: Map<String, Any>,
                detaljer: Map<String, Any>,
            ) {}
        }
    private val mediator =
        GodkjenningMediator(
            vedtakDao = mockk(relaxed = true),
            opptegnelseDao = opptegnelseDao,
            oppgaveDao = mockk(relaxed = true),
            utbetalingDao = mockk(relaxed = true),
            meldingDao =
                mockk(relaxed = true) {
                    every { finnUtbetalingsgodkjenningbehovJson(any()) } returns "{}"
                    every { finnFødselsnummer(any()) } returns fnr
                },
            generasjonDao = mockk(relaxed = true),
        )

    private val saksbehandler =
        Saksbehandlerløsning.Saksbehandler(
            ident = "saksbehandlerident",
            epostadresse = "saksbehandler@nav.no",
        )

    private val beslutter =
        Saksbehandlerløsning.Saksbehandler(
            ident = "beslutterident",
            epostadresse = "beslutter@nav.no",
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
        mediator.automatiskAvvisning(
            publiserer = context::publiser,
            vedtaksperiodeId = UUID.randomUUID(),
            begrunnelser = listOf("foo"),
            utbetaling = utbetaling,
            hendelseId = UUID.randomUUID(),
            spleisBehandlingId = null,
        )
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    @Test
    fun `automatisk utbetaling skal opprette opptegnelse`() {
        mediator.automatiskUtbetaling(
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            hendelseId = UUID.randomUUID(),
            spleisBehandlingId = null,
        )
        assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet()
    }

    @Test
    fun `saksbehandler utbetaling skal ikke opprette opptegnelse`() {
        mediator.saksbehandlerUtbetaling(
            behandlingId = UUID.randomUUID(),
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
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1.januar, listOf(generasjon())),
            spleisBehandlingId = null,
        )
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `saksbehandler utbetaling med spleisBehandlingId`() {
        val spleisBehandlingId = UUID.randomUUID()
        mediator.saksbehandlerUtbetaling(
            behandlingId = UUID.randomUUID(),
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
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1.januar, listOf(generasjon())),
            spleisBehandlingId = spleisBehandlingId,
        )
        val hendelser = hendelserInspektør.hendelser("vedtaksperiode_godkjent")
        assertEquals(1, hendelser.size)
        val vedtaksperiodeGodkjent = hendelser.single()
        assertEquals(spleisBehandlingId, vedtaksperiodeGodkjent["behandlingId"]?.asUUID())
    }

    @Test
    fun `saksbehandler utbetaling uten spleisBehandlingId`() {
        mediator.saksbehandlerUtbetaling(
            behandlingId = UUID.randomUUID(),
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
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1.januar, listOf(generasjon())),
            spleisBehandlingId = null,
        )
        val hendelser = hendelserInspektør.hendelser("vedtaksperiode_godkjent")
        assertEquals(1, hendelser.size)
        val vedtaksperiodeGodkjent = hendelser.single()
        assertEquals(null, vedtaksperiodeGodkjent["behandlingId"]?.asUUID())
    }

    @Test
    fun `legg saksbehandler og beslutter på vedtaksperiode_godkjent`() {
        mediator.saksbehandlerUtbetaling(
            behandlingId = UUID.randomUUID(),
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
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1.januar, listOf(generasjon())),
            spleisBehandlingId = null,
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
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1.januar, listOf(generasjon())),
            spleisBehandlingId = null,
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
            årsak = null,
            begrunnelser = emptyList(),
            kommentar = null,
            saksbehandleroverstyringer = emptyList(),
            spleisBehandlingId = null,
        )
        val hendelser = hendelserInspektør.hendelser("vedtaksperiode_avvist")
        val vedtaksperiodeAvvist = hendelser.single()
        assertEquals(saksbehandler.ident, vedtaksperiodeAvvist["saksbehandler"]["ident"].asText())
        assertEquals(saksbehandler.epostadresse, vedtaksperiodeAvvist["saksbehandler"]["epostadresse"].asText())
        assertEquals(null, vedtaksperiodeAvvist["beslutter"]?.get("ident")?.asText())
        assertEquals(null, vedtaksperiodeAvvist["beslutter"]?.get("epostadresse")?.asText())
        assertEquals(false, vedtaksperiodeAvvist["automatiskBehandling"].asBoolean())
    }

    @Test
    fun `legg på tbd-saksbehandler på vedtaksperiode_godkjent ved automatisk utbetaling`() {
        mediator.automatiskUtbetaling(
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            hendelseId = UUID.randomUUID(),
            spleisBehandlingId = null,
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
            begrunnelser = emptyList(),
            utbetaling = utbetaling,
            hendelseId = UUID.randomUUID(),
            spleisBehandlingId = null,
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
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            saksbehandlerIdent = "1",
            saksbehandlerEpost = "2@nav.no",
            saksbehandler = saksbehandler,
            godkjenttidspunkt = LocalDateTime.now(),
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            saksbehandleroverstyringer = emptyList(),
            spleisBehandlingId = null,
        )
        assertOpptegnelseIkkeOpprettet()
    }

    @Test
    fun `saksbehandler avvisning med spleisBehandlingId`() {
        val spleisBehandlingId = UUID.randomUUID()
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
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            saksbehandleroverstyringer = emptyList(),
            spleisBehandlingId = spleisBehandlingId,
        )
        val hendelser = hendelserInspektør.hendelser("vedtaksperiode_avvist")
        val vedtaksperiodeAvvist = hendelser.single()
        assertEquals(spleisBehandlingId, vedtaksperiodeAvvist["behandlingId"]?.asUUID())
    }

    @Test
    fun `saksbehandler avvisning uten spleisBehandlingId`() {
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
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            saksbehandleroverstyringer = emptyList(),
            spleisBehandlingId = null,
        )
        val hendelser = hendelserInspektør.hendelser("vedtaksperiode_avvist")
        val vedtaksperiodeAvvist = hendelser.single()
        assertEquals(null, vedtaksperiodeAvvist["behandlingId"]?.asUUID())
    }

    @Test
    fun `godkjenner varsler for alle gjeldende generasjoner`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId1, vedtaksperiodeId1)
        val generasjon2 = generasjon(generasjonId2, vedtaksperiodeId2)
        val varsel1 = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId1)
        val varsel2 = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId2)
        generasjon1.håndterNyttVarsel(varsel1)
        generasjon2.håndterNyttVarsel(varsel2)

        godkjenning(listOf(generasjon1, generasjon2))
        generasjon1.inspektør {
            assertEquals(1, varsler.size)
            assertEquals(vedtaksperiodeId1, varsler.first().vedtaksperiodeId)
        }
        generasjon2.inspektør {
            assertEquals(1, varsler.size)
            assertEquals(vedtaksperiodeId2, varsler.first().vedtaksperiodeId)
        }
    }

    private fun generasjon(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
    ) = Generasjon(
        id = id,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = 1.januar,
        tom = 31.januar,
        skjæringstidspunkt = 1.januar,
    )

    private fun godkjenning(generasjoner: List<Generasjon>) =
        mediator.saksbehandlerUtbetaling(
            behandlingId = UUID.randomUUID(),
            context = context,
            behov = UtbetalingsgodkjenningMessage("{}", utbetaling),
            vedtaksperiodeId = UUID.randomUUID(),
            fødselsnummer = fnr,
            saksbehandlerIdent = "Z000000",
            saksbehandlerEpost = "saksbehandler@nav.no",
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            godkjenttidspunkt = LocalDateTime.now(),
            saksbehandleroverstyringer = emptyList(),
            sykefraværstilfelle = Sykefraværstilfelle(fnr, 1.januar, generasjoner),
            spleisBehandlingId = null,
        )

    private fun assertFerdigbehandletGodkjenningsbehovOpptegnelseOpprettet() =
        verify(exactly = 1) {
            opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseType.FERDIGBEHANDLET_GODKJENNINGSBEHOV))
        }

    private fun assertOpptegnelseIkkeOpprettet() =
        verify(exactly = 0) {
            opptegnelseDao.opprettOpptegnelse(eq(fnr), any(), eq(OpptegnelseType.NY_SAKSBEHANDLEROPPGAVE))
        }

    private companion object {
        const val fnr = "12341231221"
    }
}
