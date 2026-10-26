package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PgGraderteAndreYtelserRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = sessionContext.graderteAndreYtelserRepository

    @Test
    fun `kan lagre og hente graderte andre ytelser`() {
        // Given:
        val identitetsnummer = lagIdentitetsnummer()
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat til beslutter",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserType = GraderteAndreYtelserType.PLEIEPENGER,
            )

        // When:
        repository.lagre(graderteAndreYtelser)

        val hentet = repository.finnOrNull(graderteAndreYtelser.id)
        val hentetForPerson = repository.finnAlleForIdentitetsnummer(identitetsnummer)
        val lagretType =
            dbQuery.single(
                """
                SELECT type FROM graderte_andre_ytelser_events
                WHERE graderte_andre_ytelser_id = :graderte_andre_ytelser_id
                """.trimIndent(),
                "graderte_andre_ytelser_id" to graderteAndreYtelser.id.value,
            ) { it.string("type") }

        // Then:
        assertNotNull(hentet)
        assertEquals(graderteAndreYtelser.id, hentet.id)
        assertEquals(identitetsnummer, hentet.identitetsnummer)
        assertEquals(GraderteAndreYtelserType.PLEIEPENGER, hentet.graderteAndreYtelserType)
        assertEquals(1, hentet.versjon)
        assertEquals(listOf(graderteAndreYtelser.id), hentetForPerson.map { it.id })
        assertEquals("OPPRETTET", lagretType)
    }

    @Test
    fun `lagre er idempotent for allerede persisterte events`() {
        // Given:
        val graderteAndreYtelser =
            GraderteAndreYtelser.ny(
                identitetsnummer = lagIdentitetsnummer(),
                saksbehandlerIdent = lagSaksbehandler().ident,
                notatTilBeslutter = "notat til beslutter",
                totrinnsvurderingId = TotrinnsvurderingId(Random.nextLong()),
                graderteAndreYtelserType = GraderteAndreYtelserType.FORELDREPENGER,
            )

        // When:
        repository.lagre(graderteAndreYtelser)
        repository.lagre(graderteAndreYtelser)

        val antallRader =
            dbQuery.single(
                """
                SELECT COUNT(*) AS antall FROM graderte_andre_ytelser_events
                WHERE graderte_andre_ytelser_id = :graderte_andre_ytelser_id
                """.trimIndent(),
                "graderte_andre_ytelser_id" to graderteAndreYtelser.id.value,
            ) { it.int("antall") }

        // Then:
        assertEquals(1, antallRader)
    }
}
