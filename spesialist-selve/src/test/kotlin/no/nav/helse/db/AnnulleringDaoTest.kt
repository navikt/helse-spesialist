package no.nav.helse.db

import DatabaseIntegrationTest
import TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnnulleringDaoTest: DatabaseIntegrationTest() {

    @BeforeEach
    fun tømTabeller() {
        query("truncate annullert_av_saksbehandler, begrunnelse cascade").execute()
    }
    @Test
    fun `Lagrer annullering med begrunnelse`() {
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        annulleringDao.lagreAnnullering(annulleringDto(), saksbehandler())
        assertAnnullering()
        assertBegrunnelse()
    }

    @Test
    fun `Lagrer annullering uten begrunnelse`() {
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        annulleringDao.lagreAnnullering(annulleringDto(begrunnelse = null), saksbehandler())
        assertAnnullering()
        assertIngenBegrunnelse()
    }

    @Test
    fun `kan finne annullering med begrunnelse`() {
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        annulleringDao.lagreAnnullering(annulleringDto(), saksbehandler())
        val annullering = annulleringDao.finnAnnullering(UTBETALING_ID)
        assertEquals(UTBETALING_ID, annullering?.utbetalingId)
        assertEquals(SAKSBEHANDLER_IDENT, annullering?.saksbehandlerIdent)
        assertNotNull(annullering?.begrunnelse)
    }

    @Test
    fun `kan finne annullering uten begrunnelse`() {
        saksbehandlerDao.opprettSaksbehandler(SAKSBEHANDLER_OID, SAKSBEHANDLER_NAVN, SAKSBEHANDLER_EPOST, SAKSBEHANDLER_IDENT)
        annulleringDao.lagreAnnullering(annulleringDto(begrunnelse = null), saksbehandler())
        val annullering = annulleringDao.finnAnnullering(UTBETALING_ID)
        assertEquals(UTBETALING_ID, annullering?.utbetalingId)
        assertEquals(SAKSBEHANDLER_IDENT, annullering?.saksbehandlerIdent)
        assertNull(annullering?.begrunnelse)
    }

    private fun annulleringDto(begrunnelse: String? = "annulleringsbegrunnelse") =
        AnnulleringDto(
            aktørId = AKTØR,
            fødselsnummer = FNR,
            organisasjonsnummer = ORGNUMMER,
            vedtaksperiodeId = VEDTAKSPERIODE,
            utbetalingId = UTBETALING_ID,
            årsaker = listOf("en årsak", "to årsak"),
            kommentar = begrunnelse,
        )

    private fun saksbehandler() =
        Saksbehandler(
            epostadresse = SAKSBEHANDLER_EPOST,
            oid = SAKSBEHANDLER_OID,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
        )

    private fun assertAnnullering() {
        val annullering =
            query(
                """select * from annullert_av_saksbehandler where utbetaling_id = :utbetalingId""",
                "utbetalingId" to UTBETALING_ID,
            ).single {
                mapOf(
                    "vedtaksperiodeId" to it.uuid("vedtaksperiode_id"),
                    "utbetalingId" to it.uuid("utbetaling_id"),
                    "årsaker" to it.array<String>("årsaker").toList(),
                )
            }
        assertEquals(VEDTAKSPERIODE, annullering?.get("vedtaksperiodeId"))
        assertEquals(UTBETALING_ID, annullering?.get("utbetalingId"))
        assertEquals(listOf("en årsak", "to årsak"), annullering?.get("årsaker"))
    }

    private fun assertBegrunnelse() {
        val begrunnelse =
            query(
                """select * from begrunnelse where type = 'ANNULLERING'""",
            ).single {
                mapOf(
                    "tekst" to it.string("tekst"),
                    "type" to it.string("type"),
                    "saksbehandler" to it.uuid("saksbehandler_ref"),
                )
            }

        assertEquals("annulleringsbegrunnelse", begrunnelse?.get("tekst"))
        assertEquals("ANNULLERING", begrunnelse?.get("type"))
        assertEquals(SAKSBEHANDLER_OID, begrunnelse?.get("saksbehandler"))
    }

    private fun assertIngenBegrunnelse() {
        val begrunnelse =
            query(
                """select * from begrunnelse where type = 'ANNULLERING'""",
            ).single {
                mapOf(
                    "tekst" to it.string("tekst"),
                    "type" to it.string("type"),
                    "saksbehandler" to it.uuid("saksbehandler_ref"),
                )
            }
        assertNull(begrunnelse)
    }
}
