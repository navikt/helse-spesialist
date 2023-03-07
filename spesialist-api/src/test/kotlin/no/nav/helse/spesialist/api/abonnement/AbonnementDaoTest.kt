package no.nav.helse.spesialist.api.abonnement

import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AbonnementDaoTest : DatabaseIntegrationTest() {

    private val abonnementDao = AbonnementDao(dataSource)

    @Test
    fun `får ikke nytt sekvensnummer hvis det allerede fins et`() {
        val saksbehandlerId = opprettSaksbehandler()
        val eksisterendeSekvensnummer = 234
        settSekvensnummer(saksbehandlerId, eksisterendeSekvensnummer)

        abonnementDao.opprettAbonnement(saksbehandlerId, 1L)

        assertEquals(eksisterendeSekvensnummer, finnSekvensnummer(saksbehandlerId))
    }

    @Test
    fun `får satt nyeste sekvensnummer for personen hvis saksbehandler aldri tidligere har opprettet abonnement`() {
        val saksbehandlerId = opprettSaksbehandler()
        val sekvensnummerForPersonen = 9696
        val aktørId = "33"
        val personId = opprettPerson(aktørId = aktørId)
        lagOpptegnelse(personId, sekvensnummerForPersonen)

        abonnementDao.opprettAbonnement(saksbehandlerId, aktørId.toLong())

        assertEquals(sekvensnummerForPersonen, finnSekvensnummer(saksbehandlerId))
    }

    @Test
    fun `får satt nyeste globale sekvensnummer hvis verken person eller saksbehandler har noen`() {
        val saksbehandlerId = opprettSaksbehandler()
        val sekvensnummerForEnUrelatertPerson = 4095
        val endaEtSekvensnummerForEnUrelatertPerson = 4096
        val personId = opprettPerson()
        lagOpptegnelse(personId, sekvensnummerForEnUrelatertPerson)
        lagOpptegnelse(personId, endaEtSekvensnummerForEnUrelatertPerson)

        abonnementDao.opprettAbonnement(saksbehandlerId, AKTØRID.toLong())

        assertEquals(endaEtSekvensnummerForEnUrelatertPerson, finnSekvensnummer(saksbehandlerId))
    }

    @Test
    fun `sekvensnummer blir 0 når det ikke fins noen opptegnelser for personen`() {
        val saksbehandlerId = opprettSaksbehandler()
        val aktørId = "42"
        opprettPerson(aktørId = aktørId)

        abonnementDao.opprettAbonnement(saksbehandlerId, aktørId.toLong())

        assertEquals(0, finnSekvensnummer(saksbehandlerId))
    }

    @Test
    fun `nyeste sekvensnummer for personen settes når saksbehandler oppretter abonnement`() {
        val saksbehandlerId = opprettSaksbehandler()
        val sekvensnummerForPersonen = 9696
        val aktørId = "42"
        val personId = opprettPerson(aktørId = aktørId)
        lagOpptegnelse(personId, sekvensnummerForPersonen)

        abonnementDao.opprettAbonnement(saksbehandlerId, aktørId.toLong())

        assertEquals(sekvensnummerForPersonen, finnSekvensnummer(saksbehandlerId))
    }

    private fun settSekvensnummer(saksbehandlerId: UUID, sekvensnummer: Int) {
        @Language("postgresql")
        val query = """
            insert into saksbehandler_opptegnelse_sekvensnummer
            values ('$saksbehandlerId', $sekvensnummer)
        """
        sessionOf(dataSource).use { it.run(queryOf(query).asUpdate) }
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
            from saksbehandler_opptegnelse_sekvensnummer
            where saksbehandler_id = :saksbehandlerId
        """, "saksbehandlerId" to saksbehandlerId
    ).single { it.intOrNull("siste_sekvensnummer") }
}
