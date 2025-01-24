package no.nav.helse.db.api

import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.parallel.Isolated
import java.util.UUID

@Isolated
internal class PgAbonnementApiDaoTest : DatabaseIntegrationTest() {

    private val abonnementDao = PgAbonnementApiDao(dataSource)

    @Test
    fun `får nytt sekvensnummer selvom det allerede fins et`() {
        val saksbehandlerId = opprettSaksbehandler()
        settSekvensnummer(saksbehandlerId, 42)

        val aktørId = lagAktørId()
        val personId = opprettPerson(aktørId = aktørId)
        lagOpptegnelse(personId, 962)

        abonnementDao.opprettAbonnement(saksbehandlerId, aktørId)

        assertEquals(962, finnSekvensnummer(saksbehandlerId))
    }

    @Test
    fun `får satt nyeste globale sekvensnummer hvis verken person eller saksbehandler har noen`() {
        val saksbehandlerId = opprettSaksbehandler()
        val sekvensnummerForEnUrelatertPerson = 4095
        val endaEtSekvensnummerForEnUrelatertPerson = 4096
        val personId = opprettPerson()
        lagOpptegnelse(personId, sekvensnummerForEnUrelatertPerson)
        lagOpptegnelse(personId, endaEtSekvensnummerForEnUrelatertPerson)

        abonnementDao.opprettAbonnement(saksbehandlerId, AKTØRID)

        assertEquals(endaEtSekvensnummerForEnUrelatertPerson, finnSekvensnummer(saksbehandlerId))
    }

    @Test
    fun `alle tidligere abonnement slettes når et nytt opprettes`() {
        val saksbehandlerId = opprettSaksbehandler()
        val aktørId1 = lagAktørId()
        val aktørId2 = lagAktørId()
        opprettPerson(aktørId = aktørId1)
        opprettPerson(aktørId = aktørId2, fødselsnummer = lagFødselsnummer())

        abonnementDao.opprettAbonnement(saksbehandlerId, aktørId1)
        assertEquals(listOf(aktørId1), finnPersonerSaksbehandlerAbonnererPå(saksbehandlerId))
        abonnementDao.opprettAbonnement(saksbehandlerId, aktørId2)
        assertEquals(listOf(aktørId2), finnPersonerSaksbehandlerAbonnererPå(saksbehandlerId))
    }

    @Test
    fun `fungerer også for personer med flere fødselsnumre`() {
        val saksbehandlerId = opprettSaksbehandler()
        val aktørId1 = lagAktørId()
        val dNummer = lagFødselsnummer()
        val fødselsnummer = lagFødselsnummer()
        opprettPerson(aktørId = aktørId1, fødselsnummer = dNummer)
        opprettPerson(aktørId = aktørId1, fødselsnummer = fødselsnummer)

        assertDoesNotThrow {
            abonnementDao.opprettAbonnement(saksbehandlerId, aktørId1)
        }
    }

    private fun settSekvensnummer(saksbehandlerId: UUID, sekvensnummer: Int) = dbQuery.update(
        """
            insert into saksbehandler_opptegnelse_sekvensnummer
            values ('$saksbehandlerId', $sekvensnummer)
        """.trimIndent()
    )

    private fun lagOpptegnelse(personRef: Long, eksisterendeSekvensnummer: Int) = dbQuery.update(
        """
            insert into opptegnelse
            values (:person_id, :sekvensnummer, '{"innhold": "noe oppdateringsrelatert"}', 'en eller annen opptegnelsestype')
        """.trimIndent(), "person_id" to personRef, "sekvensnummer" to eksisterendeSekvensnummer
    )

    private fun finnSekvensnummer(saksbehandlerId: UUID) = dbQuery.single(
        """
            select siste_sekvensnummer
            from saksbehandler_opptegnelse_sekvensnummer
            where saksbehandler_id = :saksbehandlerId
        """.trimIndent(),
        "saksbehandlerId" to saksbehandlerId
    ) { it.intOrNull("siste_sekvensnummer") }

    private fun finnPersonerSaksbehandlerAbonnererPå(saksbehandlerId: UUID) = dbQuery.list(
        """
            select aktør_id
            from abonnement_for_opptegnelse
            join person p on abonnement_for_opptegnelse.person_id = p.id
            where saksbehandler_id = :saksbehandlerId
        """.trimIndent(),
        "saksbehandlerId" to saksbehandlerId
    ) { it.string("aktør_id") }
}
