package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringArsak
import no.nav.helse.modell.saksbehandler.handlinger.AnnulleringDto
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.legacy.LegacySaksbehandler
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
        val årsaker = setOf("en årsak", "to årsak")
        annulleringRepository.lagreAnnullering(
            annulleringDto(
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                årsaker = årsaker,
            ),
            saksbehandler(),
        )
        val annullering = annulleringRepository.finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId) ?: fail()
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
        annulleringRepository.lagreAnnullering(
            annulleringDto(
                begrunnelse = null,
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                årsaker = emptySet() // Vi burde kanskje egentlig ha validering på at årsaker må ha innhold.. 🤔
            ),
            saksbehandler(),
        )
        val annullering = annulleringRepository.finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)
        assertEquals(arbeidsgiverFagsystemId, annullering?.arbeidsgiverFagsystemId)
        assertEquals(personFagsystemId, annullering?.personFagsystemId)
        assertEquals(SAKSBEHANDLER_IDENT, annullering?.saksbehandlerIdent)
        assertNull(annullering?.begrunnelse)
    }

    @Test
    fun `kan lagre og finne annullering med vedtaksperiodeId`() {
        val arbeidsgiverFagsystemId = "EN-ARBEIDSGIVER-FAGSYSTEMID3"
        val personFagsystemId = "EN-PERSON-FAGSYSTEMID3"
        val vedtaksperiodeId = UUID.randomUUID()
        opprettSaksbehandler()
        annulleringRepository.lagreAnnullering(
            annulleringDto(
                begrunnelse = null,
                arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
                personFagsystemId = personFagsystemId,
                årsaker = setOf("en årsak", "to årsak"),
                vedtaksperiodeId = vedtaksperiodeId,
            ),
            saksbehandler(),
        )
        val annullering = annulleringRepository.finnAnnullering(arbeidsgiverFagsystemId, personFagsystemId)
        assertEquals(vedtaksperiodeId, annullering?.vedtaksperiodeId)
    }

    private fun annulleringDto(
        begrunnelse: String? = "annulleringsbegrunnelse",
        utbetalingId: UUID = UTBETALING_ID,
        arbeidsgiverFagsystemId: String = "EN-ARBEIDSGIVER-FAGSYSTEMID",
        personFagsystemId: String = "EN-PERSON-FAGSYSTEMID",
        årsaker: Collection<String>,
        vedtaksperiodeId: UUID = VEDTAKSPERIODE,
    ) = AnnulleringDto(
        aktørId = AKTØR,
        fødselsnummer = FNR,
        organisasjonsnummer = ORGNUMMER,
        vedtaksperiodeId = vedtaksperiodeId,
        utbetalingId = utbetalingId,
        arbeidsgiverFagsystemId = arbeidsgiverFagsystemId,
        personFagsystemId = personFagsystemId,
        årsaker = årsaker.mapIndexed { i, årsak -> AnnulleringArsak("key$i", årsak) },
        kommentar = begrunnelse,
    )

    private fun saksbehandler(saksbehandlerOid: UUID = SAKSBEHANDLER_OID) =
        LegacySaksbehandler(
            epostadresse = SAKSBEHANDLER_EPOST,
            oid = saksbehandlerOid,
            navn = SAKSBEHANDLER_NAVN,
            ident = SAKSBEHANDLER_IDENT,
        )
}
