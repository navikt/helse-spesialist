package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.api.person.Kjønn
import org.intellij.lang.annotations.Language

internal class PersonDao(private val dataSource: DataSource) {
    internal fun findPersonByFødselsnummer(fødselsnummer: String): Long? = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT id FROM person WHERE fodselsnummer=?;"
        session.run(
            queryOf(query, fødselsnummer.toLong())
                .map { row -> row.long("id") }
                .asSingle
        )
    }

    internal fun finnAktørId(fødselsnummer: String): String? = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT aktor_id FROM person WHERE fodselsnummer = ?"
        session.run(queryOf(query, fødselsnummer.toLong()).map { it.string("aktor_id") }.asSingle)
    }

    internal fun findPersoninfoSistOppdatert(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT personinfo_oppdatert FROM person WHERE fodselsnummer=?;"
        session.run(
            queryOf(query, fødselsnummer.toLong())
                .map { row -> row.localDateOrNull("personinfo_oppdatert") }
                .asSingle
        )
    }

    internal fun findPersoninfoRef(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT info_ref FROM person WHERE fodselsnummer=?;"
        session.run(
            queryOf(query, fødselsnummer.toLong())
                .map { row -> row.longOrNull("info_ref") }
                .asSingle
        )
    }

    internal fun insertPersoninfo(
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO person_info(fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
            VALUES(:fornavn, :mellomnavn, :etternavn, :fodselsdato, CAST(:kjonn as person_kjonn), :adressebeskyttelse);
        """
        requireNotNull(
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fornavn" to fornavn,
                        "mellomnavn" to mellomnavn,
                        "etternavn" to etternavn,
                        "fodselsdato" to fødselsdato,
                        "kjonn" to kjønn.name,
                        "adressebeskyttelse" to adressebeskyttelse.name
                    )
                ).asUpdateAndReturnGeneratedKey
            )
        )
    }

    fun upsertPersoninfo(
        fødselsnummer: String,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        session.transaction { transaction ->
            transaction.finnPersonInfoRef(fødselsnummer)
                ?.also {
                    transaction.updatePersonInfo(
                        it,
                        fornavn,
                        mellomnavn,
                        etternavn,
                        fødselsdato,
                        kjønn,
                        adressebeskyttelse,
                        fødselsnummer
                    )
                }
                ?: transaction.insertPersoninfo(
                    fornavn,
                    mellomnavn,
                    etternavn,
                    fødselsdato,
                    kjønn,
                    adressebeskyttelse,
                    fødselsnummer
                )
        }
    }

    private fun TransactionalSession.finnPersonInfoRef(fødselsnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT info_ref FROM person WHERE fodselsnummer=?"

        return run(queryOf(query, fødselsnummer.toLong()).map { it.longOrNull("info_ref") }.asSingle)
    }

    private fun TransactionalSession.finnPersonRef(fødselsnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT id FROM person WHERE fodselsnummer=?"

        return run(queryOf(query, fødselsnummer.toLong()).map { it.longOrNull("id") }.asSingle)
    }

    private fun TransactionalSession.updatePersonInfo(
        id: Long,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
        fødselsnummer: String
    ) {
        @Language("PostgreSQL")
        val query = """
            UPDATE person_info 
            SET fornavn=:fornavn, 
                mellomnavn=:mellomnavn, 
                etternavn=:etternavn, 
                fodselsdato=:fodselsdato, 
                kjonn=CAST(:kjonn as person_kjonn), 
                adressebeskyttelse=:adressebeskyttelse 
            WHERE id=:id""".trimIndent()

        run(
            queryOf(
                query,
                mapOf(
                    "fornavn" to fornavn,
                    "mellomnavn" to mellomnavn,
                    "etternavn" to etternavn,
                    "fodselsdato" to fødselsdato,
                    "kjonn" to kjønn.name,
                    "adressebeskyttelse" to adressebeskyttelse.name,
                    "id" to id
                )
            ).asUpdate
        )
        updatePersoninfoOppdatert(fødselsnummer)
    }

    private fun TransactionalSession.insertPersoninfo(
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
        fødselsnummer: String
    ) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO person_info(fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
            VALUES(:fornavn, :mellomnavn, :etternavn, :fodselsdato, CAST(:kjonn as person_kjonn), :adressebeskyttelse);
        """

        val personinfoId = requireNotNull(
            run(
                queryOf(
                    query,
                    mapOf(
                        "fornavn" to fornavn,
                        "mellomnavn" to mellomnavn,
                        "etternavn" to etternavn,
                        "fodselsdato" to fødselsdato,
                        "kjonn" to kjønn.name,
                        "adressebeskyttelse" to adressebeskyttelse.name
                    )
                ).asUpdateAndReturnGeneratedKey
            )
        )
        updatePersoninfoRef(personinfoId, fødselsnummer)
    }

    private fun TransactionalSession.updatePersoninfoRef(id: Long, fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "UPDATE person SET info_ref=:id, personinfo_oppdatert=now() WHERE fodselsnummer=:fodselsnummer"

        run(queryOf(query, mapOf("id" to id, "fodselsnummer" to fødselsnummer.toLong())).asUpdate)
    }

    private fun TransactionalSession.updatePersoninfoOppdatert(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "UPDATE person SET personinfo_oppdatert=now() WHERE fodselsnummer=?"

        run(queryOf(query, fødselsnummer.toLong()).asUpdate)
    }

    internal fun findAdressebeskyttelse(fødselsnummer: String): Adressebeskyttelse? =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val adressebeskyttelseQuery = """
            SELECT adressebeskyttelse FROM person_info
            WHERE id=(SELECT info_ref FROM person WHERE fodselsnummer=?);
        """
            session.run(
                queryOf(
                    adressebeskyttelseQuery,
                    fødselsnummer.toLong()
                ).map { row -> Adressebeskyttelse.valueOf(row.string("adressebeskyttelse")) }.asSingle
            )
        }

    internal fun findEnhetSistOppdatert(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT enhet_ref_oppdatert FROM person WHERE fodselsnummer=?;"
        session.run(
            queryOf(query, fødselsnummer.toLong()).map { row ->
                row.localDateOrNull("enhet_ref_oppdatert")
            }.asSingle
        )
    }

    internal fun updateEnhet(fødselsnummer: String, enhetNr: Int) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query =
            "UPDATE person SET enhet_ref=:enhetNr, enhet_ref_oppdatert=now() WHERE fodselsnummer=:fodselsnummer;"
        session.run(
            queryOf(
                query,
                mapOf(
                    "enhetNr" to enhetNr,
                    "fodselsnummer" to fødselsnummer.toLong()
                )
            ).asUpdate
        )
    }

    internal fun findInfotrygdutbetalinger(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT data FROM infotrygdutbetalinger
            WHERE id=(SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer=?);
        """
        session.run(
            queryOf(query, fødselsnummer.toLong())
                .map { row -> row.string("data") }
                .asSingle
        )
    }

    internal fun findITUtbetalingsperioderSistOppdatert(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT infotrygdutbetalinger_oppdatert FROM person WHERE fodselsnummer=?;"
        session.run(
            queryOf(query, fødselsnummer.toLong())
                .map { row -> row.localDateOrNull("infotrygdutbetalinger_oppdatert") }
                .asSingle
        )
    }

    internal fun insertInfotrygdutbetalinger(data: JsonNode) =
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            @Language("PostgreSQL")
            val query = "INSERT INTO infotrygdutbetalinger(data) VALUES(CAST(? as json));"
            requireNotNull(
                session.run(
                    queryOf(query, objectMapper.writeValueAsString(data)).asUpdateAndReturnGeneratedKey
                )
            )
        }

    internal fun findInntekter(fødselsnummer: String, skjæringstidspunkt: LocalDate) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
                SELECT * FROM inntekt 
                WHERE person_ref=(SELECT id FROM person WHERE fodselsnummer=:fodselsnummer)
                AND skjaeringstidspunkt=:skjaeringstidspunkt;
            """
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "fodselsnummer" to fødselsnummer.toLong(),
                        "skjaeringstidspunkt" to skjæringstidspunkt
                    )
                ).map { row -> objectMapper.readValue<List<Inntekter>>(row.string("inntekter"))}.asSingle
            )
        }

    internal fun insertInntekter(fødselsnummer: String, skjæringstidspunkt: LocalDate, inntekter: List<Inntekter>) =
        sessionOf(dataSource).use { session ->
            session.transaction { transaction ->
                transaction.finnPersonRef(fødselsnummer)?.also {
                    @Language("PostgreSQL")
                    val query = """
                        INSERT INTO inntekt (person_ref, skjaeringstidspunkt, inntekter)
                        VALUES (:person_ref, :skjaeringstidspunkt, :inntekter::json)
                    """
                    transaction.run(
                        queryOf(
                            query,
                            mapOf(
                                "person_ref" to it,
                                "skjaeringstidspunkt" to skjæringstidspunkt,
                                "inntekter" to objectMapper.writeValueAsString(inntekter)
                            )
                        ).asExecute
                    )
                }
            }
        }

    fun upsertInfotrygdutbetalinger(
        fødselsnummer: String,
        utbetalinger: JsonNode
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        session.transaction { transaction ->
            transaction.finnInfotrygdutbetalingerRef(fødselsnummer)
                ?.also {
                    transaction.updateInfotrygdutbetalinger(it, fødselsnummer, utbetalinger)
                }
                ?: transaction.insertInfotrygdubetalinger(utbetalinger, fødselsnummer)
        }
    }

    private fun TransactionalSession.finnInfotrygdutbetalingerRef(fødselsnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer=?"

        return run(queryOf(query, fødselsnummer.toLong()).map { it.longOrNull("infotrygdutbetalinger_ref") }.asSingle)
    }

    private fun TransactionalSession.updateInfotrygdutbetalinger(
        infotrygdutbetalingerId: Long,
        fødselsnummer: String,
        utbetalinger: JsonNode
    ) {
        @Language("PostgreSQL")
        val query = """
                UPDATE infotrygdutbetalinger SET data=CAST(:utbetalinger as json)
                WHERE id=:infotrygdutbetalingerId;
            """

        run(
            queryOf(
                query,
                mapOf(
                    "utbetalinger" to objectMapper.writeValueAsString(utbetalinger),
                    "infotrygdutbetalingerId" to infotrygdutbetalingerId
                )
            ).asUpdate
        )
        updateInfotrygdutbetalingerOppdatert(fødselsnummer)
    }

    private fun TransactionalSession.updateInfotrygdutbetalingerOppdatert(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "UPDATE person SET infotrygdutbetalinger_oppdatert=now() WHERE fodselsnummer=?"

        run(queryOf(query, fødselsnummer.toLong()).asUpdate)
    }

    private fun TransactionalSession.insertInfotrygdubetalinger(utbetalinger: JsonNode, fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO infotrygdutbetalinger (data) VALUES (CAST(? as json))"

        val infotrygdutbetalingerId = requireNotNull(
            run(
                queryOf(
                    query,
                    objectMapper.writeValueAsString(utbetalinger)
                ).asUpdateAndReturnGeneratedKey
            )
        )
        updateInfotrygdutbetalingerRef(infotrygdutbetalingerId, fødselsnummer)
    }

    private fun TransactionalSession.updateInfotrygdutbetalingerRef(
        infotrygdutbetalingerId: Long,
        fødselsnummer: String
    ) {
        @Language("PostgreSQL")
        val query = """
                UPDATE person SET infotrygdutbetalinger_ref=:infotrygdutbetalingerId, infotrygdutbetalinger_oppdatert=now()
                WHERE fodselsnummer=:fodselsnummer;
            """

        run(
            queryOf(
                query,
                mapOf(
                    "infotrygdutbetalingerId" to infotrygdutbetalingerId,
                    "fodselsnummer" to fødselsnummer.toLong()

                )
            ).asUpdate
        )
    }

    internal fun insertPerson(
        fødselsnummer: String,
        aktørId: String,
        personinfoId: Long,
        enhetId: Int,
        infotrygdutbetalingerId: Long
    ) = requireNotNull(sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref, enhet_ref_oppdatert, personinfo_oppdatert, infotrygdutbetalinger_oppdatert)
            VALUES(:fodselsnummer, :aktorId, :personinfoId, :enhetId, :infotrygdutbetalingerId, :timestamp, :timestamp, :timestamp);
        """
        session.run(
            queryOf(
                query,
                mapOf(
                    "fodselsnummer" to fødselsnummer.toLong(),
                    "aktorId" to aktørId.toLong(),
                    "personinfoId" to personinfoId,
                    "enhetId" to enhetId,
                    "infotrygdutbetalingerId" to infotrygdutbetalingerId,
                    "timestamp" to LocalDateTime.now()
                )
            ).asUpdateAndReturnGeneratedKey
        )
    })

    internal fun insertPerson(
        fødselsnummer: String,
        aktørId: String,
    ) = requireNotNull(sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO person(fodselsnummer, aktor_id)
            VALUES(:fodselsnummer, :aktorId);
        """
        session.run(
            queryOf(
                query,
                mapOf(
                    "fodselsnummer" to fødselsnummer.toLong(),
                    "aktorId" to aktørId.toLong()
                )
            ).asUpdateAndReturnGeneratedKey
        )
    })

    internal fun finnEnhetId(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "SELECT enhet_ref FROM person where fodselsnummer = ?;"
        requireNotNull(session.run(queryOf(statement, fødselsnummer.toLong()).map {
            it.int("enhet_ref").toEnhetnummer()
        }.asSingle))
    }

    internal fun markerPersonSomKlarForVisning(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "INSERT INTO stottetabell_for_skjonnsmessig_fastsettelse(fodselsnummer) VALUES(:fodselsnummer);"
        session.run(queryOf(statement, mapOf("fodselsnummer" to fødselsnummer.toLong())).asUpdate)
    }
}

internal fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
private fun Int.toEnhetnummer() = if (this < 1000) "0$this" else this.toString()
