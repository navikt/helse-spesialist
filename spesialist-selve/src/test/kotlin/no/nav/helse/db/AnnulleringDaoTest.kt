package no.nav.helse.db

import DatabaseIntegrationTest
import TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringArsak
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class AnnulleringDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `kan finne annullering med begrunnelse`() {
        val arbeidsgiverFagsystemId = "EN-ARBEIDSGIVER-FAGSYSTEMID1"
        val personFagsystemId = "EN-PERSON-FAGSYSTEMID1"
        nyPerson()
        utbetalingsopplegg(1000, 0)
        opprettSaksbehandler()
        annulleringDao.lagreAnnullering(
            annulleringDto(
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
            ),
            saksbehandler(),
        )
        annulleringDao.finnAnnulleringId(arbeidsgiverFagsystemId)?.let { annulleringId ->
            utbetalingDao.leggTilAnnullertAvSaksbehandler(UTBETALING_ID, annulleringId)
        }
        val annullering = annulleringDao.finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)
        assertEquals(UTBETALING_ID, annullering?.utbetalingId)
        assertEquals(SAKSBEHANDLER_IDENT, annullering?.saksbehandlerIdent)
        assertNotNull(annullering?.begrunnelse)
    }

    @Test
    fun `kan finne annullering uten begrunnelse`() {
        val arbeidsgiverFagsystemId = "EN-ARBEIDSGIVER-FAGSYSTEMID2"
        val personFagsystemId = "EN-PERSON-FAGSYSTEMID2"
        nyPerson()
        utbetalingsopplegg(1000, 0)
        opprettSaksbehandler()
        annulleringDao.lagreAnnullering(
            annulleringDto(begrunnelse = null, arbeidsgiverFagsystemId = arbeidsgiverFagsystemId, personFagsystemId = personFagsystemId),
            saksbehandler(),
        )
        annulleringDao.finnAnnulleringId(arbeidsgiverFagsystemId)?.let { annulleringId ->
            utbetalingDao.leggTilAnnullertAvSaksbehandler(UTBETALING_ID, annulleringId)
        }
        val annullering = annulleringDao.finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)
        assertEquals(UTBETALING_ID, annullering?.utbetalingId)
        assertEquals(SAKSBEHANDLER_IDENT, annullering?.saksbehandlerIdent)
        assertNull(annullering?.begrunnelse)
    }

    private fun annulleringDto(
        begrunnelse: String? = "annulleringsbegrunnelse",
        utbetalingId: UUID = UTBETALING_ID,
        arbeidsgiverFagsystemId: String = "EN-ARBEIDSGIVER-FAGSYSTEMID",
        personFagsystemId: String = "EN-PERSON-FAGSYSTEMID",
    ) = AnnulleringDto(
        aktørId = AKTØR,
        fødselsnummer = FNR,
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = VEDTAKSPERIODE,
        utbetalingId = utbetalingId,
        arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
        personFagsystemId = personFagsystemId,
        årsaker = listOf(AnnulleringArsak("key1", "en årsak"), AnnulleringArsak("key2", "to årsak")),
        kommentar = begrunnelse,
    )

    private fun saksbehandler(saksbehandlerOid: UUID = SAKSBEHANDLER_OID) =
        Saksbehandler(
            epostadresse = SAKSBEHANDLER_EPOST,
            oid = saksbehandlerOid,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT,
            tilgangskontroll = TilgangskontrollForTestHarIkkeTilgang,
        )
}
