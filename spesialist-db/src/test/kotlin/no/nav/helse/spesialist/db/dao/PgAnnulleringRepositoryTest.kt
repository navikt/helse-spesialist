package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.Annullering
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.util.UUID

class PgAnnulleringRepositoryTest : AbstractDBIntegrationTest() {
    @Test
    fun `kan finne annullering med begrunnelse og årsaker`() {
        val arbeidsgiverFagsystemId = "EN-ARBEIDSGIVER-FAGSYSTEMID1"
        val personFagsystemId = "EN-PERSON-FAGSYSTEMID1"
        opprettSaksbehandler()
        val årsaker = listOf("en årsak", "to årsak")
        annulleringRepository.lagreAnnullering(
            annullering(
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                årsaker = årsaker,
            ),
        )
        val annullering = annulleringRepository.finnAnnulleringMedEnAv(arbeidsgiverFagsystemId, personFagsystemId) ?: fail()
        assertEquals(arbeidsgiverFagsystemId, annullering.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, annullering.personFagsystemId)
        assertEquals(SAKSBEHANDLER_OID, annullering.saksbehandlerOid.value)
        assertNotNull(annullering.kommentar)
        assertEquals(årsaker, annullering.årsaker)
    }

    @Test
    fun `kan finne annullering uten begrunnelse`() {
        val arbeidsgiverFagsystemId = "EN-ARBEIDSGIVER-FAGSYSTEMID2"
        val personFagsystemId = "EN-PERSON-FAGSYSTEMID2"
        opprettSaksbehandler()
        annulleringRepository.lagreAnnullering(
            annullering(
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                årsaker = emptyList(),
                begrunnelse = null // Vi burde kanskje egentlig ha validering på at årsaker må ha innhold.. 🤔
            ),
        )
        val annullering = annulleringRepository.finnAnnulleringMedEnAv(arbeidsgiverFagsystemId, personFagsystemId)
        assertEquals(arbeidsgiverFagsystemId, annullering?.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, annullering?.personFagsystemId)
        assertEquals(SAKSBEHANDLER_OID, annullering?.saksbehandlerOid?.value)
        assertNull(annullering?.kommentar)
    }

    @Test
    fun `kan lagre og finne annullering med vedtaksperiodeId`() {
        val arbeidsgiverFagsystemId = "EN-ARBEIDSGIVER-FAGSYSTEMID3"
        val personFagsystemId = "EN-PERSON-FAGSYSTEMID3"
        val vedtaksperiodeId = UUID.randomUUID()
        opprettSaksbehandler()
        annulleringRepository.lagreAnnullering(
            annullering(
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                vedtaksperiodeId = vedtaksperiodeId,
                årsaker = listOf("en årsak", "to årsak"),
                begrunnelse = null,
            ),
        )
        val annullering = annulleringRepository.finnAnnullering(vedtaksperiodeId)
        assertEquals(vedtaksperiodeId, annullering?.vedtaksperiodeId)
        assertEquals(arbeidsgiverFagsystemId, annullering?.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, annullering?.personFagsystemId)
        assertEquals(SAKSBEHANDLER_OID, annullering?.saksbehandlerOid?.value)
    }

    private fun annullering(
        arbeidsgiverFagsystemId: String = "EN-ARBEIDSGIVER-FAGSYSTEMID",
        personFagsystemId: String = "EN-PERSON-FAGSYSTEMID",
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
        årsaker: List<String>,
        begrunnelse: String? = "annulleringsbegrunnelse",
    ) = Annullering.Factory.ny(
        arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
        personFagsystemId = personFagsystemId,
        saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
        vedtaksperiodeId = vedtaksperiodeId,
        årsaker = årsaker,
        kommentar = begrunnelse,
    )
}
