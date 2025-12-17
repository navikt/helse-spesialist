package no.nav.helse.spesialist.db.dao.api
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagAktørId
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.parallel.Isolated
import java.util.UUID

@Isolated
internal class PgAbonnementApiDaoTest : AbstractDBIntegrationTest() {
    @Test
    fun `får nytt sekvensnummer selvom det allerede fins et`() {
        val saksbehandlerId = opprettSaksbehandler()
        settSekvensnummer(saksbehandlerId, 42)

        val person = opprettPerson()
        lagOpptegnelse(person.id.value, 962)

        abonnementDao.opprettAbonnement(saksbehandlerId, person.aktørId)

        assertEquals(962, finnSekvensnummer(saksbehandlerId))
    }

    @Test
    fun `får satt nyeste globale sekvensnummer hvis verken person eller saksbehandler har noen`() {
        val saksbehandlerId = opprettSaksbehandler()
        val sekvensnummerForEnUrelatertPerson = 4095
        val endaEtSekvensnummerForEnUrelatertPerson = 4096
        val person = opprettPerson()
        lagOpptegnelse(person.id.value, sekvensnummerForEnUrelatertPerson)
        lagOpptegnelse(person.id.value, endaEtSekvensnummerForEnUrelatertPerson)

        abonnementDao.opprettAbonnement(saksbehandlerId, person.aktørId)

        assertEquals(endaEtSekvensnummerForEnUrelatertPerson, finnSekvensnummer(saksbehandlerId))
    }

    @Test
    fun `alle tidligere abonnement slettes når et nytt opprettes`() {
        val saksbehandlerId = opprettSaksbehandler()
        val person1 = opprettPerson()
        val person2 = opprettPerson()

        abonnementDao.opprettAbonnement(saksbehandlerId, person1.aktørId)
        assertEquals(listOf(person1.aktørId), finnPersonerSaksbehandlerAbonnererPå(saksbehandlerId))
        abonnementDao.opprettAbonnement(saksbehandlerId, person2.aktørId)
        assertEquals(listOf(person2.aktørId), finnPersonerSaksbehandlerAbonnererPå(saksbehandlerId))
    }

    @Test
    fun `fungerer også for personer med flere fødselsnumre`() {
        val saksbehandlerId = opprettSaksbehandler()
        val aktørId1 = lagAktørId()
        opprettPerson(lagPerson(aktørId = aktørId1))
        opprettPerson(lagPerson(aktørId = aktørId1))

        assertDoesNotThrow {
            abonnementDao.opprettAbonnement(saksbehandlerId, aktørId1)
        }
    }

    private fun settSekvensnummer(
        saksbehandlerId: UUID,
        sekvensnummer: Int,
    ) = dbQuery.update(
        """
        insert into saksbehandler_opptegnelse_sekvensnummer
        values ('$saksbehandlerId', $sekvensnummer)
        """.trimIndent(),
    )

    private fun lagOpptegnelse(
        fødselsnummer: String,
        eksisterendeSekvensnummer: Int,
    ) = dbQuery.update(
        """
        insert into opptegnelse
        select id, :sekvensnummer, '{"innhold": "noe oppdateringsrelatert"}', 'en eller annen opptegnelsestype'
        from person where person.fødselsnummer = :foedselsnummer
        """.trimIndent(),
        "foedselsnummer" to fødselsnummer,
        "sekvensnummer" to eksisterendeSekvensnummer,
    )

    private fun finnSekvensnummer(saksbehandlerId: UUID) =
        dbQuery.single(
            """
            select siste_sekvensnummer
            from saksbehandler_opptegnelse_sekvensnummer
            where saksbehandler_id = :saksbehandlerId
            """.trimIndent(),
            "saksbehandlerId" to saksbehandlerId,
        ) { it.intOrNull("siste_sekvensnummer") }

    private fun finnPersonerSaksbehandlerAbonnererPå(saksbehandlerId: UUID) =
        dbQuery.list(
            """
            select aktør_id
            from abonnement_for_opptegnelse
            join person p on abonnement_for_opptegnelse.person_id = p.id
            where saksbehandler_id = :saksbehandlerId
            """.trimIndent(),
            "saksbehandlerId" to saksbehandlerId,
        ) { it.string("aktør_id") }
}
