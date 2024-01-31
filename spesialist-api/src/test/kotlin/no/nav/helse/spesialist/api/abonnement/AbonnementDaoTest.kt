package no.nav.helse.spesialist.api.abonnement

import java.util.UUID
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AbonnementDaoTest : DatabaseIntegrationTest() {

    private val abonnementDao = AbonnementDao(dataSource)

    @Test
    fun `får nytt sekvensnummer selvom det allerede fins et`() {
        val saksbehandlerId = opprettSaksbehandler()
        settSekvensnummer(saksbehandlerId, 42)

        opprettPerson(aktørId = "91", fødselsnummer = "123")
        val personId = opprettPerson(aktørId = "97")
        lagOpptegnelse(personId, 962)

        abonnementDao.opprettAbonnement(saksbehandlerId, 97)

        assertEquals(962, finnSekvensnummer(saksbehandlerId))
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

    @Test
    fun `alle tidligere abonnement slettes når et nytt opprettes`() {
        val saksbehandlerId = opprettSaksbehandler()
        val aktørId1 = "42"
        val aktørId2 = "43"
        opprettPerson(aktørId = aktørId1)
        opprettPerson(aktørId = aktørId2, fødselsnummer = "12121299999")

        abonnementDao.opprettAbonnement(saksbehandlerId, aktørId1.toLong())
        assertEquals(listOf(aktørId1), finnPersonerSaksbehandlerAbonnererPå(saksbehandlerId))
        abonnementDao.opprettAbonnement(saksbehandlerId, aktørId2.toLong())
        assertEquals(listOf(aktørId2), finnPersonerSaksbehandlerAbonnererPå(saksbehandlerId))
    }

    private fun settSekvensnummer(saksbehandlerId: UUID, sekvensnummer: Int) = query(
        """
            insert into saksbehandler_opptegnelse_sekvensnummer
            values ('$saksbehandlerId', $sekvensnummer)
        """.trimIndent()
    ).update()

    private fun lagOpptegnelse(personRef: Long, eksisterendeSekvensnummer: Int) = query(
        """
            insert into opptegnelse
            values (:person_id, :sekvensnummer, '{"innhold": "noe oppdateringsrelatert"}', 'en eller annen opptegnelsestype')
        """.trimIndent(), "person_id" to personRef, "sekvensnummer" to eksisterendeSekvensnummer
    ).update()

    private fun finnSekvensnummer(saksbehandlerId: UUID) = query(
        """
            select siste_sekvensnummer
            from saksbehandler_opptegnelse_sekvensnummer
            where saksbehandler_id = :saksbehandlerId
        """.trimIndent(), "saksbehandlerId" to saksbehandlerId
    ).single { it.intOrNull("siste_sekvensnummer") }

    private fun finnPersonerSaksbehandlerAbonnererPå(saksbehandlerId: UUID) = query(
        """
            select aktor_id
            from abonnement_for_opptegnelse
            join person p on abonnement_for_opptegnelse.person_id = p.id
            where saksbehandler_id = :saksbehandlerId
        """.trimIndent(), "saksbehandlerId" to saksbehandlerId
    ).list { it.string("aktor_id") }
}
