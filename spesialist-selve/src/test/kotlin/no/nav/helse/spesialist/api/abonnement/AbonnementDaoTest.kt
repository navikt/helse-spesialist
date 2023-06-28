package no.nav.helse.spesialist.api.abonnement

import DatabaseIntegrationTest
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AbonnementDaoTest : DatabaseIntegrationTest() {

    @Test
    fun `sekvensnummer blir null når det ikke fins noen opptegnelser for personen`() {
        val saksbehandlerId = opprettSaksbehandler()
        val aktørId = "42"
        opprettPerson(aktørId = aktørId)

        abonnementDao.opprettAbonnement(saksbehandlerId, aktørId.toLong())

        assertEquals(null, finnSekvensnummer(saksbehandlerId))
    }

    @Test
    fun `nyeste sekvensnummer for personen settes når saksbehandler oppretter abonnement`() {
        val saksbehandlerId = opprettSaksbehandler()
        val sekvensnummerForPersonen = 9696
        val aktørId = "42"
        val personId = opprettPerson(aktørId = aktørId)
        lagOpptegnelse(personId.personId, sekvensnummerForPersonen)

        abonnementDao.opprettAbonnement(saksbehandlerId, aktørId.toLong())

        assertEquals(sekvensnummerForPersonen, finnSekvensnummer(saksbehandlerId))
    }

    private fun lagOpptegnelse(personRef: Long, eksisterendeSekvensnummer: Int) = query(
        """
            insert into opptegnelse
            values (:person_id, :sekvensnummer, '{"innhold": "noe oppdateringsrelatert"}', 'en eller annen opptegnelsestype')
        """, "person_id" to personRef, "sekvensnummer" to eksisterendeSekvensnummer
    ).update()

    private fun finnSekvensnummer(saksbehandlerId: UUID) = query(
        """
            select siste_sekvensnummer
            from abonnement_for_opptegnelse
            where saksbehandler_id = :saksbehandlerId
        """, "saksbehandlerId" to saksbehandlerId
    ).single { it.intOrNull("siste_sekvensnummer") }
}
