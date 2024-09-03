package no.nav.helse.mediator.api

import AbstractIntegrationTest
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import no.nav.helse.spesialist.test.lagEtternavn
import no.nav.helse.spesialist.test.lagFornavn
import no.nav.helse.spesialist.test.lagSaksbehandlerident
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.util.UUID

internal class GodkjenningServiceTest : AbstractIntegrationTest() {

    @Test
    fun `oppgave avventer system når saksbehandlerløsning legges på rapid`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        håndterSaksbehandlerløsning()
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = false)
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSystem)
    }

    @Test
    fun `håndter godkjenning`() {
        val saksbehandler = enSaksbehandler()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()

        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", saksbehandler)
        assertSaksbehandlerløsning(godkjent = true, automatiskBehandlet = false, totrinnsvurdering = false)
    }

    @Test
    fun `håndter godkjenning med beslutteroppgave`() {
        val opprinneligSaksbehandler = enSaksbehandler()
        val beslutter = enSaksbehandler()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()

        settTotrinnsvurdering(opprinneligSaksbehandler = opprinneligSaksbehandler, beslutter = beslutter)

        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", beslutter)
        assertSaksbehandlerløsning(godkjent = true, automatiskBehandlet = false, totrinnsvurdering = true)
    }

    @Test
    fun `Ved godkjenning av beslutteroppgave reserveres personen til tidligereSaksbehandler`() {
        val opprinneligSaksbehandler = enSaksbehandler()
        val beslutterOid = enSaksbehandler()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()

        settTotrinnsvurdering(opprinneligSaksbehandler = opprinneligSaksbehandler, beslutter = beslutterOid)

        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", beslutterOid)

        assertReservertTil(opprinneligSaksbehandler)
    }

    @Test
    fun `For beslutteroppgave reserveres personen til opprinnelig saksbehandler, uavhengig av hvem som godkjenner`() {
        val opprinneligSaksbehandler = enSaksbehandler()
        val beslutter = enSaksbehandler()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()

        settTotrinnsvurdering(opprinneligSaksbehandler, beslutter)

        val enTredjeSaksbehandler = enSaksbehandler()
        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", enTredjeSaksbehandler)

        assertReservertTil(opprinneligSaksbehandler)
    }

    @Test
    fun `Hvis oppgaven ikke er beslutteroppgave, reserveres person til opprinnelig saksbehandler`() {
        val opprinneligSaksbehandler = enSaksbehandler()
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()

        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", opprinneligSaksbehandler)

        assertReservertTil(opprinneligSaksbehandler)
    }

    @Test
    fun `Lagrer at beslutter har attestert i periodehistorikk`() {
        val opprinneligSaksbehandler = enSaksbehandler()
        val beslutter = enSaksbehandler()

        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()

        settTotrinnsvurdering(opprinneligSaksbehandler, beslutter)

        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", beslutter)

        val utbetalingId = oppgaveDao.finnUtbetalingId(1.oppgave(VEDTAKSPERIODE_ID)) ?: fail("Fant ikke utbetalingId")
        assertPeriodehistorikk(utbetalingId)
    }

    @Test
    fun `Takler at det mangler data på totrinnsvurderingen ved avvisning`() {
        val opprinneligSaksbehandler = enSaksbehandler()

        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        opprettInitiellTotrinnsvurdering()
        godkjenningService.håndter(godkjenningDtoAvvisning(), "epost@nav.no", opprinneligSaksbehandler)

        assertSaksbehandlerløsning(godkjent = false, automatiskBehandlet = false, totrinnsvurdering = false)
    }

    @Test
    fun `Sender ikke med beslutter ved avvisning`() {
        val opprinneligSaksbehandler = enSaksbehandler()
        val beslutter = enSaksbehandler()

        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        spesialistBehandlerGodkjenningsbehovFremTilOppgave()
        settTotrinnsvurdering(opprinneligSaksbehandler, beslutter)
        godkjenningService.håndter(godkjenningDtoAvvisning(), "epost@nav.no", opprinneligSaksbehandler)

        assertSaksbehandlerløsning(godkjent = false, automatiskBehandlet = false, totrinnsvurdering = false)
    }

    private fun enSaksbehandler(): UUID = UUID.randomUUID().also(::opprettSaksbehandler)

    // Per nå blir det opprettet "en totrinnsvurdering" på et tidspunkt, som så blir updated med uuid-er på et senere
    // tidspunkt
    private fun opprettInitiellTotrinnsvurdering() {
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(fødselsnummer = FØDSELSNUMMER)

        @Language("postgresql") val sql = "insert into totrinnsvurdering (vedtaksperiode_id) values (:vedtaksperiodeId)"
        sessionOf(dataSource).use { session ->
            session.run(queryOf(sql, mapOf("vedtaksperiodeId" to vedtaksperiodeId)).asUpdate)
        }
    }

    private fun settTotrinnsvurdering(opprinneligSaksbehandler: UUID?, beslutter: UUID?) {
        val vedtaksperiodeId = oppgaveDao.finnVedtaksperiodeId(fødselsnummer = FØDSELSNUMMER)

        @Language("postgresql") val sql = """
            insert into totrinnsvurdering (
                vedtaksperiode_id,
                er_retur,
                saksbehandler,
                beslutter
            )
            values (:vedtaksperiodeId, :erRetur, :opprinneligSaksbehandler, :beslutter)
        """
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    sql, mapOf(
                        "vedtaksperiodeId" to vedtaksperiodeId,
                        "erRetur" to false,
                        "opprinneligSaksbehandler" to opprinneligSaksbehandler,
                        "beslutter" to beslutter,
                    )
                ).asUpdate
            )
        }
    }

    private fun godkjenningDto(oppgaveId: Long = sisteOppgaveId()) =
        GodkjenningDto(oppgaveId, true, "saksbehandler", null, null, null)

    private fun godkjenningDtoAvvisning(oppgaveId: Long = sisteOppgaveId()) =
        GodkjenningDto(oppgaveId, false, "saksbehandler", "en årsak", null, null)

    private fun opprettSaksbehandler(oid: UUID) {
        @Language("PostgreSQL") val query =
            " insert into saksbehandler(oid, navn, epost, ident) values (:oid, :navn, :e_post, :ident ) "
        return sessionOf(dataSource).use { session ->
            val fornavn = lagFornavn()
            val etternavn = lagEtternavn()
            session.run(
                queryOf(
                    query, mapOf(
                        "oid" to oid,
                        "navn" to "$fornavn $etternavn",
                        "e_post" to "$fornavn.$etternavn@nav.no".lowercase(),
                        "ident" to lagSaksbehandlerident(),
                    )
                ).asUpdate
            )
        }
    }

    private fun assertReservertTil(oid: UUID) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL") val query = " select * from reserver_person where saksbehandler_ref = :oid "
        val reservert =
            session.run(queryOf(query, mapOf("oid" to oid)).map { true }.asSingle) ?: fail("Fant ikke reservasjon")
        assertTrue(reservert, "Personen skal være reservert til $oid")
    }

    private fun assertPeriodehistorikk(utbetalingId: UUID) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL") val query = " select * from periodehistorikk where utbetaling_id = :utbetaling_id "
        val type =
            session.run(queryOf(query, mapOf("utbetaling_id" to utbetalingId)).map { it.string("type") }.asSingle)
                ?: fail("Fant ikke reservasjon")
        assertEquals("TOTRINNSVURDERING_ATTESTERT", type)
    }

}
