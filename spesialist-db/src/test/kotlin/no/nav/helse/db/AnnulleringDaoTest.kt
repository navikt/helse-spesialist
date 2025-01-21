package no.nav.helse.db

import no.nav.helse.DatabaseIntegrationTest
import no.nav.helse.util.TilgangskontrollForTestHarIkkeTilgang
import no.nav.helse.modell.saksbehandler.Saksbehandler
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringArsak
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.util.UUID

class AnnulleringDaoTest : DatabaseIntegrationTest() {
    @Test
    fun `kan finne annullering med begrunnelse og årsaker`() {
        val arbeidsgiverFagsystemId = "EN-ARBEIDSGIVER-FAGSYSTEMID1"
        val personFagsystemId = "EN-PERSON-FAGSYSTEMID1"
        opprettSaksbehandler()
        val årsaker = setOf("en årsak", "to årsak")
        annulleringDao.lagreAnnullering(
            annulleringDto(
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                årsaker = årsaker,
            ),
            saksbehandler(),
        )
        val annullering = annulleringDao.finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId) ?: fail()
        assertEquals(arbeidsgiverFagsystemId, annullering.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, annullering.personFagsystemId)
        assertEquals(SAKSBEHANDLER_IDENT, annullering.saksbehandlerIdent)
        assertNotNull(annullering.begrunnelse)
        assertEquals(årsaker, annullering.arsaker.toSet())
    }

    @Test
    fun `kan finne annullering uten begrunnelse`() {
        val arbeidsgiverFagsystemId = "EN-ARBEIDSGIVER-FAGSYSTEMID2"
        val personFagsystemId = "EN-PERSON-FAGSYSTEMID2"
        opprettSaksbehandler()
        annulleringDao.lagreAnnullering(
            annulleringDto(
                begrunnelse = null,
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                årsaker = emptySet() // Vi burde kanskje egentlig ha validering på at årsaker må ha innhold.. 🤔
            ),
            saksbehandler(),
        )
        val annullering = annulleringDao.finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)
        assertEquals(arbeidsgiverFagsystemId, annullering?.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, annullering?.personFagsystemId)
        assertEquals(SAKSBEHANDLER_IDENT, annullering?.saksbehandlerIdent)
        assertNull(annullering?.begrunnelse)
    }

    private fun annulleringDto(
        begrunnelse: String? = "annulleringsbegrunnelse",
        utbetalingId: UUID = UTBETALING_ID,
        arbeidsgiverFagsystemId: String = "EN-ARBEIDSGIVER-FAGSYSTEMID",
        personFagsystemId: String = "EN-PERSON-FAGSYSTEMID",
        årsaker: Collection<String>,
    ) = AnnulleringDto(
        aktørId = AKTØR,
        fødselsnummer = FNR,
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = VEDTAKSPERIODE,
        utbetalingId = utbetalingId,
        arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
        personFagsystemId = personFagsystemId,
        årsaker = årsaker.mapIndexed { i, årsak -> AnnulleringArsak("key$i", årsak) },
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
