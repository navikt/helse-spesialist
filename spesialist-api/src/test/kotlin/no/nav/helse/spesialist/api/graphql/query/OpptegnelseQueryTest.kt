package no.nav.helse.spesialist.api.graphql.query

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.helse.spesialist.api.AbstractGraphQLApiTest
import no.nav.helse.spesialist.api.SaksbehandlerMediator
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelse
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelsetype
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelsetype.REVURDERING_FERDIGBEHANDLET
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelsetype.UTBETALING_ANNULLERING_FEILET
import no.nav.helse.spesialist.api.graphql.schema.Opptegnelsetype.UTBETALING_ANNULLERING_OK
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OpptegnelseQueryTest: AbstractGraphQLApiTest() {
    private val testRapid = TestRapid()
    override val saksbehandlerMediator: SaksbehandlerMediator = SaksbehandlerMediator(dataSource, testRapid)

    @Test
    fun `hent opptegnelser uten sekvensId`() {
        opprettPerson()
        val typer = listOf(
            UTBETALING_ANNULLERING_FEILET,
            UTBETALING_ANNULLERING_OK,
            REVURDERING_FERDIGBEHANDLET
        )
        abonner(AKTØRID)
        opprettOpptegnelse(FØDSELSNUMMER, typer[0])
        opprettOpptegnelse(FØDSELSNUMMER, typer[1])
        opprettOpptegnelse(FØDSELSNUMMER, typer[2])

        val body = runQuery(
            """query HentOpptegnelser {
                hentOpptegnelser {
                    aktorId
                    sekvensnummer
                    type
                    payload
                }
            }"""
        )
        val opptegnelser = jacksonObjectMapper().treeToValue<List<Opptegnelse>>(body["data"]["hentOpptegnelser"])
        assertEquals(3, opptegnelser.size)
        opptegnelser.forEachIndexed { index, opptegnelse ->
            assertEquals(AKTØRID, opptegnelse.aktorId)
            assertEquals(index + 1, opptegnelse.sekvensnummer)
            assertEquals("""{}""", opptegnelse.payload)
            assertEquals(typer[index], opptegnelse.type)
        }
    }

    @Test
    fun `hent opptegnelser med sekvensId`() {
        opprettPerson()
        val typer = listOf(
            UTBETALING_ANNULLERING_FEILET,
            UTBETALING_ANNULLERING_OK,
            REVURDERING_FERDIGBEHANDLET
        )
        opprettOpptegnelse(FØDSELSNUMMER, typer[0])
        opprettOpptegnelse(FØDSELSNUMMER, typer[1])
        opprettOpptegnelse(FØDSELSNUMMER, typer[2])
        abonner(AKTØRID)
        val body = runQuery(
            """query HentOpptegnelser {
                hentOpptegnelser(sekvensId: 2) {
                    aktorId
                    sekvensnummer
                    type
                    payload
                }
            }"""
        )

        val opptegnelser = jacksonObjectMapper().treeToValue<List<Opptegnelse>>(body["data"]["hentOpptegnelser"])
        assertEquals(1, opptegnelser.size)
        val opptegnelse = opptegnelser.single()
        assertEquals(AKTØRID, opptegnelse.aktorId)
        assertEquals(3, opptegnelse.sekvensnummer)
        assertEquals("""{}""", opptegnelse.payload)
        assertEquals(typer[2], opptegnelse.type)
    }

    private fun opprettOpptegnelse(fødselsnummer: String, type: Opptegnelsetype) {
        @Language("PostgreSQL")
        val query = """
           INSERT INTO opptegnelse(person_id, payload, type) VALUES ((SELECT id FROM person WHERE fodselsnummer = ?), ?::json, ?)
        """
        sessionOf(dataSource).use {
            it.run(queryOf(query, fødselsnummer.toLong(), "{}", type.toString()).asUpdate)
        }
    }

    private fun abonner(personId: String) {
        runQuery(
            """mutation Abonner {
                abonner(personidentifikator: "$personId")
            }"""
        )
    }
}