package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.mediator.meldinger.HentEnhetløsning.Companion.erEnhetUtland
import no.nav.helse.objectMapper
import no.nav.helse.person.Kjønn
import no.nav.helse.vedtaksperiode.EnhetDto
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.sql.DataSource

internal class PersonDao(private val dataSource: DataSource) {
    internal fun findPersonByFødselsnummer(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT id FROM person WHERE fodselsnummer=?;"
        session.run(
            queryOf(query, fødselsnummer.toLong())
                .map { row -> row.long("id") }
                .asSingle
        )
    }

    internal fun findPersoninfoSistOppdatert(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT personinfo_oppdatert FROM person WHERE fodselsnummer=?;"
        requireNotNull(
            session.run(
                queryOf(query, fødselsnummer.toLong())
                    .map { row -> row.sqlDate("personinfo_oppdatert").toLocalDate() }
                    .asSingle
            )
        )
    }

    internal fun insertPersoninfo(
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO person_info(fornavn, mellomnavn, etternavn, fodselsdato, kjonn)
            VALUES(:fornavn, :mellomnavn, :etternavn, :fodselsdato, CAST(:kjonn as person_kjonn));
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
                        "kjonn" to kjønn.name
                    )
                ).asUpdateAndReturnGeneratedKey
            )
        )
    }

    internal fun updatePersoninfo(
        fødselsnummer: String,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val personinfoQuery = """
            UPDATE person_info SET fornavn=:fornavn, mellomnavn=:mellomnavn, etternavn=:etternavn, fodselsdato=:fodselsdato, kjonn=CAST(:kjonn as person_kjonn)
            WHERE id=(SELECT info_ref FROM person WHERE fodselsnummer=:fodselsnummer);
        """
        session.run(
            queryOf(
                personinfoQuery,
                mapOf(
                    "fornavn" to fornavn,
                    "mellomnavn" to mellomnavn,
                    "etternavn" to etternavn,
                    "fodselsdato" to fødselsdato,
                    "kjonn" to kjønn.name,
                    "fodselsnummer" to fødselsnummer.toLong()
                )
            ).asUpdate
        )

        @Language("PostgreSQL")
        val personQuery = "UPDATE person SET personinfo_oppdatert=now() WHERE fodselsnummer=?;"
        session.run(
            queryOf(
                personQuery,
                fødselsnummer.toLong()
            ).asUpdate
        )
    }

    internal fun findEnhetSistOppdatert(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT enhet_ref_oppdatert FROM person WHERE fodselsnummer=?;"
        requireNotNull(
            session.run(
                queryOf(query, fødselsnummer.toLong()).map { row ->
                    row.sqlDate("enhet_ref_oppdatert").toLocalDate()
                }.asSingle
            )
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
        requireNotNull(
            session.run(
                queryOf(query, fødselsnummer.toLong())
                    .map { row -> row.sqlDate("infotrygdutbetalinger_oppdatert").toLocalDate() }
                    .asSingle
            )
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

    internal fun updateInfotrygdutbetalinger(fødselsnummer: String, data: JsonNode) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val infotrygdQuery = """
                UPDATE infotrygdutbetalinger SET data=CAST(:data as json)
                WHERE id=(SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer=:fodselsnummer);
            """
            session.run(
                queryOf(
                    infotrygdQuery,
                    mapOf(
                        "data" to objectMapper.writeValueAsString(data),
                        "fodselsnummer" to fødselsnummer.toLong()
                    )
                ).asUpdate
            )

            @Language("PostgreSQL")
            val personQuery = "UPDATE person SET infotrygdutbetalinger_oppdatert=now() WHERE fodselsnummer=?;"
            session.run(queryOf(personQuery, fødselsnummer.toLong()).asUpdate)
        }

    internal fun updateInfotrygdutbetalingerRef(fødselsnummer: String, ref: Long) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = """
                UPDATE person SET infotrygdutbetalinger_ref=:ref, infotrygdutbetalinger_oppdatert=now()
                WHERE fodselsnummer=:fodselsnummer;
            """
            session.run(
                queryOf(
                    query,
                    mapOf(
                        "ref" to ref,
                        "fodselsnummer" to fødselsnummer.toLong()
                    )
                ).asUpdate
            )
        }

    internal fun insertPerson(
        fødselsnummer: String,
        aktørId: String,
        navnId: Long,
        enhetId: Int,
        infotrygdutbetalingerId: Long
    ) = requireNotNull(sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref)
            VALUES(:fodselsnummer, :aktorId, :navnId, :enhetId, :infotrygdutbetalingerId);
        """
        session.run(
            queryOf(
                query,
                mapOf(
                    "fodselsnummer" to fødselsnummer.toLong(),
                    "aktorId" to aktørId.toLong(),
                    "navnId" to navnId,
                    "enhetId" to enhetId,
                    "infotrygdutbetalingerId" to infotrygdutbetalingerId
                )
            ).asUpdateAndReturnGeneratedKey
        )
    })

    internal fun tilhørerUtlandsenhet(fødselsnummer: String) = erEnhetUtland(findEnhet(fødselsnummer).id)

    internal fun findEnhet(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = "SELECT id, navn from enhet WHERE id=(SELECT enhet_ref FROM person where fodselsnummer =?);"
        requireNotNull(
            session.run(
                queryOf(query, fødselsnummer.toLong())
                    .map { row -> EnhetDto(
                        row.string("id"),
                        row.string("navn")
                    )
                    }
                    .asSingle
            )
        )
    }
}

internal fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
