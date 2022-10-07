package no.nav.helse.spesialist.api.snapshot

import com.fasterxml.jackson.module.kotlin.readValue
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.spesialist.api.graphql.hentsnapshot.GraphQLPerson
import no.nav.helse.spesialist.api.graphql.schema.Adressebeskyttelse
import no.nav.helse.spesialist.api.graphql.schema.Kjonn
import no.nav.helse.spesialist.api.graphql.schema.Personinfo
import no.nav.helse.spesialist.api.objectMapper
import org.intellij.lang.annotations.Language

class SnapshotApiDao(private val dataSource: DataSource) {
    fun hentSnapshotMedMetadata(fødselsnummer: String): Pair<Personinfo, GraphQLPerson>? =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                """ SELECT * FROM person AS p
                    INNER JOIN person_info as pi ON pi.id = p.info_ref
                    INNER JOIN snapshot AS s ON s.person_ref = p.id
                WHERE p.fodselsnummer = :fnr;
            """
            session.run(
                queryOf(
                    statement,
                    mapOf("fnr" to fødselsnummer.toLong())
                ).map { row ->
                    val personinfo = Personinfo(
                        fornavn = row.string("fornavn"),
                        mellomnavn = row.stringOrNull("mellomnavn"),
                        etternavn = row.string("etternavn"),
                        fodselsdato = row.stringOrNull("fodselsdato"),
                        kjonn = row.stringOrNull("kjonn")?.let(Kjonn::valueOf) ?: Kjonn.Ukjent,
                        adressebeskyttelse = row.string("adressebeskyttelse").let(Adressebeskyttelse::valueOf)
                    )
                    val snapshot = objectMapper.readValue<GraphQLPerson>(row.string("data"))
                    personinfo to snapshot
                }.asSingle
            )
        }
}