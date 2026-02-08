package no.nav.helse.spesialist.db.dao

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Annullering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.util.UUID

class PgAnnulleringRepositoryTest : AbstractDBIntegrationTest() {
    private val saksbehandler = opprettSaksbehandler()

    @Test
    fun `kan finne annullering med begrunnelse og årsaker`() {
        val arbeidsgiverFagsystemId = UUID.randomUUID().toString()
        val personFagsystemId = UUID.randomUUID().toString()
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
        assertEquals(saksbehandler.id, annullering.saksbehandlerOid)
        assertNotNull(annullering.kommentar)
        assertEquals(årsaker, annullering.årsaker)
    }

    @Test
    fun `kan finne annullering uten begrunnelse`() {
        val arbeidsgiverFagsystemId = UUID.randomUUID().toString()
        val personFagsystemId = UUID.randomUUID().toString()
        annulleringRepository.lagreAnnullering(
            annullering(
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                årsaker = emptyList(),
                begrunnelse = null, // Vi burde kanskje egentlig ha validering på at årsaker må ha innhold.. 🤔
            ),
        )
        val annullering = annulleringRepository.finnAnnulleringMedEnAv(arbeidsgiverFagsystemId, personFagsystemId)
        assertEquals(arbeidsgiverFagsystemId, annullering?.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, annullering?.personFagsystemId)
        assertEquals(saksbehandler.id, annullering?.saksbehandlerOid)
        assertNull(annullering?.kommentar)
    }

    @Test
    fun `kan lagre og finne annullering med vedtaksperiodeId`() {
        val arbeidsgiverFagsystemId = UUID.randomUUID().toString()
        val personFagsystemId = UUID.randomUUID().toString()
        val vedtaksperiodeId = UUID.randomUUID()
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
        assertEquals(saksbehandler.id, annullering?.saksbehandlerOid)
    }

    private fun annullering(
        arbeidsgiverFagsystemId: String,
        personFagsystemId: String,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        årsaker: List<String>,
        begrunnelse: String? = "annulleringsbegrunnelse",
    ) = Annullering.Factory.ny(
        arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
        personFagsystemId = personFagsystemId,
        saksbehandlerOid = saksbehandler.id,
        vedtaksperiodeId = vedtaksperiodeId,
        årsaker = årsaker,
        kommentar = begrunnelse,
    )
}
