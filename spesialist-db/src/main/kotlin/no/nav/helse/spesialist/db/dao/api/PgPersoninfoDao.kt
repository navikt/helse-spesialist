package no.nav.helse.spesialist.db.dao.api

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.api.PersoninfoDao
import no.nav.helse.db.api.PersoninfoDao.Personinfo
import no.nav.helse.db.api.PersoninfoDao.Personinfo.Adressebeskyttelse
import no.nav.helse.db.api.PersoninfoDao.Personinfo.Kjonn
import org.intellij.lang.annotations.Language
import javax.sql.DataSource

class PgPersoninfoDao internal constructor(private val dataSource: DataSource) : PersoninfoDao {
    override fun hentPersoninfo(fødselsnummer: String): Personinfo? =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val statement =
                """
                SELECT * FROM person AS p
                INNER JOIN person_info as pi ON pi.id = p.info_ref
                WHERE p.fødselsnummer = :fnr;
                """.trimIndent()
            session.run(
                queryOf(
                    statement,
                    mapOf("fnr" to fødselsnummer),
                ).map { row ->
                    Personinfo(
                        fornavn = row.string("fornavn"),
                        mellomnavn = row.stringOrNull("mellomnavn"),
                        etternavn = row.string("etternavn"),
                        fodselsdato = row.localDate("fodselsdato"),
                        kjonn = row.stringOrNull("kjonn")?.let(Kjonn::valueOf) ?: Kjonn.Ukjent,
                        adressebeskyttelse = row.string("adressebeskyttelse").let(Adressebeskyttelse::valueOf),
                    )
                }.asSingle,
            )
        }
}
