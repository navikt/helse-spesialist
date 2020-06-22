package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.EnhetDto
import no.nav.helse.objectMapper
import java.util.*
import javax.sql.DataSource

class PersonDao(private val dataSource: DataSource) {
    internal fun findPersonByFødselsnummer(fødselsnummer: Long): Int? = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf("SELECT id FROM person WHERE fodselsnummer=?;", fødselsnummer)
                .map { it.int("id") }
                .asSingle
        )
    }

    fun findVedtaksperioderByAktørId(aktørId: Long): Pair<String, List<UUID>>? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    """
                    SELECT p.fodselsnummer, json_agg(DISTINCT v.vedtaksperiode_id) AS json
                    FROM vedtak AS v
                        INNER JOIN person AS p ON v.person_ref = p.id
                    WHERE p.aktor_id = ?
                    GROUP BY p.fodselsnummer;""",
                    aktørId
                )
                    .map { row ->
                        row.long("fodselsnummer").toFødselsnummer() to
                            objectMapper.readValue<List<String>>(row.string("json")).map { UUID.fromString(it) }
                    }.asSingle
            )
        }

    internal fun findInfotrygdutbetalinger(fødselsnummer: Long): String? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT data FROM infotrygdutbetalinger WHERE id=(SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer=?);",
                    fødselsnummer
                ).map { it.string("data") }.asSingle
            )
        }


    internal fun insertNavn(fornavn: String, mellomnavn: String?, etternavn: String): Int =
        requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO person_info(fornavn, mellomnavn, etternavn) VALUES(?, ?, ?);",
                    fornavn,
                    mellomnavn,
                    etternavn
                ).asUpdateAndReturnGeneratedKey
            )
        }?.toInt())

    internal fun insertPerson(
        fødselsnummer: Long,
        aktørId: Long,
        navnId: Int,
        enhetId: Int,
        infotrygdutbetalingerId: Int
    ) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref) VALUES(?, ?, ?, ?, ?);",
                    fødselsnummer,
                    aktørId,
                    navnId,
                    enhetId,
                    infotrygdutbetalingerId
                ).asExecute
            )
        }

    internal fun insertInfotrygdutbetalinger(data: JsonNode): Int =
        requireNotNull(using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO infotrygdutbetalinger(data) VALUES(CAST(? as json));",
                    objectMapper.writeValueAsString(data)
                ).asUpdateAndReturnGeneratedKey
            )
        }?.toInt())

    internal fun updateNavn(fødselsnummer: Long, fornavn: String, mellomnavn: String?, etternavn: String) =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        "UPDATE person_info SET fornavn=?, mellomnavn=?, etternavn=? WHERE id=(SELECT info_ref FROM person WHERE fodselsnummer=?);",
                        fornavn, mellomnavn, etternavn, fødselsnummer
                    ).asUpdate
                )
                tx.run(
                    queryOf(
                        "UPDATE person SET personinfo_oppdatert=now() WHERE fodselsnummer=?;",
                        fødselsnummer
                    ).asUpdate
                )
            }
        }

    internal fun updateEnhet(fødselsnummer: Long, enhetNr: Int) = using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "UPDATE person SET enhet_ref=?, enhet_ref_oppdatert=now() WHERE fodselsnummer=?;",
                enhetNr,
                fødselsnummer
            ).asUpdate
        )
    }

    internal fun updateInfotrygdutbetalinger(fødselsnummer: Long, data: JsonNode) =
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        "UPDATE infotrygdutbetalinger SET data=CAST(? as json) WHERE id=(SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer=?);",
                        objectMapper.writeValueAsString(data), fødselsnummer
                    ).asUpdate
                )
                tx.run(
                    queryOf(
                        "UPDATE person SET infotrygdutbetalinger_oppdatert=now() WHERE fodselsnummer=?;",
                        fødselsnummer
                    ).asUpdate
                )
            }
        }

    internal fun updateInfotrygdutbetalingerRef(fødselsnummer: Long, ref: Int) =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "UPDATE person SET infotrygdutbetalinger_ref=?, infotrygdutbetalinger_oppdatert=now() WHERE fodselsnummer=?;",
                    ref,
                    fødselsnummer
                ).asUpdate
            )
        }

    internal fun findPersoninfoSistOppdatert(fødselsnummer: Long) =
        requireNotNull(using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT personinfo_oppdatert FROM person WHERE fodselsnummer=?;",
                    fødselsnummer
                ).map {
                    it.sqlDate("personinfo_oppdatert").toLocalDate()
                }.asSingle
            )
        })

    internal fun findEnhetSistOppdatert(fødselsnummer: Long) = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT enhet_ref_oppdatert FROM person WHERE fodselsnummer=?;",
                fødselsnummer
            ).map {
                it.sqlDate("enhet_ref_oppdatert").toLocalDate()
            }.asSingle
        )
    })

    internal fun findEnhet(fødselsnummer: Long): EnhetDto = requireNotNull(using(sessionOf(dataSource)) { session ->
        session.run(
            queryOf(
                "SELECT id, navn from enhet WHERE id=(SELECT enhet_ref FROM person where fodselsnummer =?);",
                fødselsnummer
            ).map {
                EnhetDto(
                    it.string("id"),
                    it.string("navn")
                )
            }.asSingle
        )
    })

    internal fun findITUtbetalingsperioderSistOppdatert(fødselsnummer: Long) =
        requireNotNull(using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT infotrygdutbetalinger_oppdatert FROM person WHERE fodselsnummer=?;",
                    fødselsnummer
                ).map {
                    it.sqlDate("infotrygdutbetalinger_oppdatert").toLocalDate()
                }.asSingle
            )
        })

    private fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()

}
