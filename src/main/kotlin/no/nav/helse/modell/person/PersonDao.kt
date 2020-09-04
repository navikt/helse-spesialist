package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.helse.modell.vedtak.EnhetDto
import no.nav.helse.objectMapper
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

internal class PersonDao(private val dataSource: DataSource) {
    internal fun findPersonByFødselsnummer(fødselsnummer: String) = using(sessionOf(dataSource)) {
        it.findPersonByFødselsnummer(fødselsnummer)
    }

    internal fun findPersoninfoSistOppdatert(fødselsnummer: String) =
        using(sessionOf(dataSource)) {
            it.findPersoninfoSistOppdatert(fødselsnummer)
        }

    internal fun insertPersoninfo(
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn
    ) = using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.insertPersoninfo(fornavn, mellomnavn, etternavn, fødselsdato, kjønn)
    }
    internal fun updatePersoninfo(
        fødselsnummer: String,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn
    ) = using(sessionOf(dataSource)) {
        it.updatePersoninfo(fødselsnummer, fornavn, mellomnavn, etternavn, fødselsdato, kjønn)
    }

    internal fun findEnhetSistOppdatert(fødselsnummer: String) = using(sessionOf(dataSource)) {
        it.findEnhetSistOppdatert(fødselsnummer)
    }

    internal fun updateEnhet(fødselsnummer: String, enhetNr: Int) = using(sessionOf(dataSource)) {
        it.updateEnhet(fødselsnummer, enhetNr)
    }

    internal fun findInfotrygdutbetalinger(fødselsnummer: String) = using(sessionOf(dataSource)) {
        it.findInfotrygdutbetalinger(fødselsnummer)
    }

    internal fun findITUtbetalingsperioderSistOppdatert(fødselsnummer: String) = using(sessionOf(dataSource)) {
        it.findITUtbetalingsperioderSistOppdatert(fødselsnummer)
    }

    internal fun insertInfotrygdutbetalinger(data: JsonNode) =
        using(sessionOf(dataSource, returnGeneratedKey = true)) {
            it.insertInfotrygdutbetalinger(data)
        }

    internal fun updateInfotrygdutbetalinger(fødselsnummer: String, data: JsonNode) = using(sessionOf(dataSource)) {
        it.updateInfotrygdutbetalinger(fødselsnummer, data)
    }

    internal fun updateInfotrygdutbetalingerRef(fødselsnummer: String, ref: Int) = using(sessionOf(dataSource)) {
        it.updateInfotrygdutbetalingerRef(fødselsnummer, ref)
    }

    internal fun insertPerson(
        fødselsnummer: String,
        aktørId: Long,
        navnId: Int,
        enhetId: Int,
        infotrygdutbetalingerId: Int
    ) = using(sessionOf(dataSource, returnGeneratedKey = true)) {
        it.insertPerson(fødselsnummer, aktørId, navnId, enhetId, infotrygdutbetalingerId)
    }
}

internal fun Session.findPersonByFødselsnummer(fødselsnummer: String): Int? = this.run(
    queryOf("SELECT id FROM person WHERE fodselsnummer=?;", fødselsnummer.toLong())
        .map { it.int("id") }
        .asSingle
)

internal fun Session.findVedtaksperioderByAktørId(aktørId: Long): Pair<String, List<UUID>>? =
    this.run(
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

internal fun Session.findInfotrygdutbetalinger(fødselsnummer: String): String? =
    this.run(
        queryOf(
            "SELECT data FROM infotrygdutbetalinger WHERE id=(SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer=?);",
            fødselsnummer.toLong()
        ).map { it.string("data") }.asSingle
    )


internal fun Session.insertPersoninfo(
    fornavn: String,
    mellomnavn: String?,
    etternavn: String,
    fødselsdato: LocalDate,
    kjønn: Kjønn
): Int =
    requireNotNull(
        this.run(
            queryOf(
                """
                    INSERT INTO person_info(fornavn, mellomnavn, etternavn, fodselsdato, kjonn)
                    VALUES(?, ?, ?, ?, CAST(? as person_kjonn));
                """,
                fornavn,
                mellomnavn,
                etternavn,
                fødselsdato,
                kjønn.name
            ).asUpdateAndReturnGeneratedKey
        )?.toInt()
    )

internal fun Session.insertPerson(
    fødselsnummer: String,
    aktørId: Long,
    navnId: Int,
    enhetId: Int,
    infotrygdutbetalingerId: Int
) =
    this.run(
        queryOf(
            "INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref) VALUES(?, ?, ?, ?, ?);",
            fødselsnummer.toLong(),
            aktørId,
            navnId,
            enhetId,
            infotrygdutbetalingerId
        ).asUpdateAndReturnGeneratedKey
    )

internal fun Session.insertInfotrygdutbetalinger(data: JsonNode): Int =
    requireNotNull(
        this.run(
            queryOf(
                "INSERT INTO infotrygdutbetalinger(data) VALUES(CAST(? as json));",
                objectMapper.writeValueAsString(data)
            ).asUpdateAndReturnGeneratedKey
        )
            ?.toInt()
    )

internal fun Session.updatePersoninfo(
    fødselsnummer: String,
    fornavn: String,
    mellomnavn: String?,
    etternavn: String,
    fødselsdato: LocalDate,
    kjønn: Kjønn
): Int {
    @Language("PostgreSQL")
    val query =
        """
            UPDATE person_info SET fornavn=?, mellomnavn=?, etternavn=?, fodselsdato=?, kjonn=CAST(? as person_kjonn)
            WHERE id=(SELECT info_ref FROM person WHERE fodselsnummer=?);
        """
    run(
        queryOf(
            query,
            fornavn, mellomnavn, etternavn, fødselsdato, kjønn.name, fødselsnummer.toLong()
        ).asUpdate
    )
    return run(
        queryOf(
            "UPDATE person SET personinfo_oppdatert=now() WHERE fodselsnummer=?;",
            fødselsnummer.toLong()
        ).asUpdate
    )
}

internal fun Session.updateEnhet(fødselsnummer: String, enhetNr: Int) = this.run(
    queryOf(
        "UPDATE person SET enhet_ref=?, enhet_ref_oppdatert=now() WHERE fodselsnummer=?;",
        enhetNr,
        fødselsnummer.toLong()
    ).asUpdate
)

internal fun Session.updateInfotrygdutbetalinger(fødselsnummer: String, data: JsonNode): Int {
    run(
        queryOf(
            "UPDATE infotrygdutbetalinger SET data=CAST(? as json) WHERE id=(SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer=?);",
            objectMapper.writeValueAsString(data), fødselsnummer.toLong()
        ).asUpdate
    )
    return run(
        queryOf(
            "UPDATE person SET infotrygdutbetalinger_oppdatert=now() WHERE fodselsnummer=?;",
            fødselsnummer.toLong()
        ).asUpdate
    )
}

internal fun Session.updateInfotrygdutbetalingerRef(fødselsnummer: String, ref: Int) =
    this.run(
        queryOf(
            "UPDATE person SET infotrygdutbetalinger_ref=?, infotrygdutbetalinger_oppdatert=now() WHERE fodselsnummer=?;",
            ref,
            fødselsnummer.toLong()
        ).asUpdate
    )

internal fun Session.findPersoninfoSistOppdatert(fødselsnummer: String) =
    requireNotNull(
        this.run(
            queryOf(
                "SELECT personinfo_oppdatert FROM person WHERE fodselsnummer=?;",
                fødselsnummer.toLong()
            ).map {
                it.sqlDate("personinfo_oppdatert").toLocalDate()
            }.asSingle
        )
    )

internal fun Session.findEnhetSistOppdatert(fødselsnummer: String) = requireNotNull(
    this.run(
        queryOf(
            "SELECT enhet_ref_oppdatert FROM person WHERE fodselsnummer=?;",
            fødselsnummer.toLong()
        ).map {
            it.sqlDate("enhet_ref_oppdatert").toLocalDate()
        }.asSingle
    )
)

internal fun Session.findEnhet(fødselsnummer: String): EnhetDto = requireNotNull(
    this.run(
        queryOf(
            "SELECT id, navn from enhet WHERE id=(SELECT enhet_ref FROM person where fodselsnummer =?);",
            fødselsnummer.toLong()
        ).map {
            EnhetDto(
                it.string("id"),
                it.string("navn")
            )
        }.asSingle
    )
)

internal fun Session.findITUtbetalingsperioderSistOppdatert(fødselsnummer: String) =
    requireNotNull(
        this.run(
            queryOf(
                "SELECT infotrygdutbetalinger_oppdatert FROM person WHERE fodselsnummer=?;",
                fødselsnummer.toLong()
            ).map {
                it.sqlDate("infotrygdutbetalinger_oppdatert").toLocalDate()
            }.asSingle
        )
    )

internal fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
