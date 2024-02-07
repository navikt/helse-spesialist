package no.nav.helse.mediator.api

import AbstractIntegrationTest
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.Testdata
import no.nav.helse.spesialist.api.oppgave.Oppgavestatus
import no.nav.helse.spesialist.api.vedtak.GodkjenningDto
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

internal class GodkjenningServiceTest : AbstractIntegrationTest() {

    @Test
    fun `oppgave avventer system når saksbehandlerløsning legges på rapid`() {
        fremTilSaksbehandleroppgave()
        håndterSaksbehandlerløsning()
        assertGodkjenningsbehovBesvart(godkjent = true, automatiskBehandlet = false)
        assertSaksbehandleroppgave(oppgavestatus = Oppgavestatus.AvventerSystem)
    }

    @Test
    fun `håndter godkjenning`() {
        fremTilSaksbehandleroppgave()

        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", UUID.randomUUID(), UUID.randomUUID())
        assertSaksbehandlerløsning(godkjent = true, automatiskBehandlet = false)
    }

    @Test
    fun `Ved godkjenning av beslutteroppgave reserveres personen til tidligereSaksbehandler`() {
        val opprinneligSaksbehandler = enSaksbehandler()
        val beslutterOid = enSaksbehandler()
        fremTilSaksbehandleroppgave()

        settTotrinnsvurdering(opprinneligSaksbehandler = opprinneligSaksbehandler, beslutter = beslutterOid)

        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", beslutterOid, UUID.randomUUID())

        assertReservertTil(opprinneligSaksbehandler)
    }

    @Test
    fun `For beslutteroppgave reserveres personen til opprinnelig saksbehandler, uavhengig av hvem som godkjenner`() {
        val opprinneligSaksbehandler = enSaksbehandler()
        val beslutter = enSaksbehandler()
        fremTilSaksbehandleroppgave()

        settTotrinnsvurdering(opprinneligSaksbehandler, beslutter)

        val enTredjeSaksbehandler = enSaksbehandler()
        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", enTredjeSaksbehandler, UUID.randomUUID())

        assertReservertTil(opprinneligSaksbehandler)
    }

    @Test
    fun `Dersom ikke beslutteroppgave reserveres person til opprinnelig saksbehandler`() {
        val opprinneligSaksbehandler = enSaksbehandler()
        fremTilSaksbehandleroppgave()

        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", opprinneligSaksbehandler, UUID.randomUUID())

        assertReservertTil(opprinneligSaksbehandler)
    }

    @Test
    fun `Lagrer at beslutter har attestert i periodehistorikk`() {
        val opprinneligSaksbehandler = enSaksbehandler()
        val beslutter = enSaksbehandler()

        fremTilSaksbehandleroppgave()

        settTotrinnsvurdering(opprinneligSaksbehandler, beslutter)

        godkjenningService.håndter(godkjenningDto(), "epost@nav.no", beslutter, UUID.randomUUID())

        val utbetalingId = oppgaveDao.finnUtbetalingId(1L) ?: fail("Fant ikke utbetalingId")
        assertPeriodehistorikk(utbetalingId)
    }

    private fun enSaksbehandler(): UUID = UUID.randomUUID().also(::opprettSaksbehandler)

    private fun settTotrinnsvurdering(opprinneligSaksbehandler: UUID, beslutter: UUID) {
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

    private fun opprettSaksbehandler(oid: UUID) {
        @Language("PostgreSQL") val query =
            " insert into saksbehandler(oid, navn, epost, ident) values (:oid, :navn, :e_post, :ident ) "
        return sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query, mapOf(
                        "oid" to oid,
                        "navn" to Testdata.SAKSBEHANDLER_NAVN,
                        "e_post" to Testdata.SAKSBEHANDLER_EPOST,
                        "ident" to Testdata.SAKSBEHANDLER_IDENT,
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
