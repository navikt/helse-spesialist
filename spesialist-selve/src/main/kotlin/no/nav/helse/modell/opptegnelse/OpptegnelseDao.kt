package no.nav.helse.modell.opptegnelse

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.abonnement.OpptegnelseType
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

internal class OpptegnelseDao(private val dataSource: DataSource) {

    internal fun opprettOpptegnelse(fødselsnummer: String, payload: OpptegnelsePayload, type: OpptegnelseType) =
        sessionOf(dataSource).use  { session ->
            @Language("PostgreSQL")
            val statement = """
                INSERT INTO opptegnelse (person_id, payload, type)
                VALUES ((SELECT id FROM person WHERE fodselsnummer=?), ?::jsonb, ?);
            """
            session.run(queryOf(statement, fødselsnummer.toLong(), payload.toJson(), type.toString()).asUpdate)
        }
}
