package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.db.PersonRepository
import no.nav.helse.db.TransactionalPersonDao
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.typer.Kjønn
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

internal class PersonDao(
    private val dataSource: DataSource,
) : PersonRepository {
    override fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto = throw UnsupportedOperationException()

    override fun lagreMinimalPerson(minimalPerson: MinimalPersonDto) {
        throw UnsupportedOperationException()
    }

    override fun personKlargjort(fødselsnummer: String) {
        sessionOf(dataSource).use { session ->
            TransactionalPersonDao(session).personKlargjort(fødselsnummer)
        }
    }

    override fun finnPersonMedFødselsnummer(fødselsnummer: String): Long? =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT id FROM person WHERE fodselsnummer=?;"
            session.run(
                queryOf(query, fødselsnummer.toLong())
                    .map { row -> row.long("id") }
                    .asSingle,
            )
        }

    internal fun finnAktørId(fødselsnummer: String): String? =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT aktor_id FROM person WHERE fodselsnummer = ?"
            session.run(queryOf(query, fødselsnummer.toLong()).map { it.string("aktor_id") }.asSingle)
        }

    override fun finnPersoninfoSistOppdatert(fødselsnummer: String) =
        sessionOf(dataSource).use { session ->
            TransactionalPersonDao(session).finnPersoninfoSistOppdatert(fødselsnummer)
        }

    override fun finnPersoninfoRef(fødselsnummer: String) =
        sessionOf(dataSource).use { session ->
            @Language("PostgreSQL")
            val query = "SELECT info_ref FROM person WHERE fodselsnummer=?;"
            session.run(
                queryOf(query, fødselsnummer.toLong())
                    .map { row -> row.longOrNull("info_ref") }
                    .asSingle,
            )
        }

    internal fun insertPersoninfo(
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
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
                        "adressebeskyttelse" to adressebeskyttelse.name,
                    ),
                ).asUpdateAndReturnGeneratedKey,
            ),
        )
    }

    override fun upsertPersoninfo(
        fødselsnummer: String,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
    ) {
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
            session.transaction { transaction ->
                TransactionalPersonDao(transaction).upsertPersoninfo(
                    fødselsnummer = fødselsnummer,
                    fornavn = fornavn,
                    mellomnavn = mellomnavn,
                    etternavn = etternavn,
                    fødselsdato = fødselsdato,
                    kjønn = kjønn,
                    adressebeskyttelse = adressebeskyttelse,
                )
            }
        }
    }

    override fun finnAdressebeskyttelse(fødselsnummer: String): Adressebeskyttelse? =
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                TransactionalPersonDao(transactionalSession).finnAdressebeskyttelse(fødselsnummer)
            }
        }

    override fun finnEnhetSistOppdatert(fødselsnummer: String) =
        sessionOf(dataSource).use { session ->
            session.transaction { transactionalSession ->
                TransactionalPersonDao(transactionalSession).finnEnhetSistOppdatert(fødselsnummer)
            }
        }

    override fun oppdaterEnhet(
        fødselsnummer: String,
        enhetNr: Int,
    ) = sessionOf(dataSource).use { session ->
        TransactionalPersonDao(session).oppdaterEnhet(fødselsnummer, enhetNr)
    }

    override fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String) =
        sessionOf(dataSource).use { TransactionalPersonDao(it).finnITUtbetalingsperioderSistOppdatert(fødselsnummer) }

    override fun finnInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ) = sessionOf(dataSource).use {
        TransactionalPersonDao(it).finnInntekter(fødselsnummer, skjæringstidspunkt)
    }

    override fun lagreInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ) = sessionOf(dataSource).use {
        TransactionalPersonDao(it).lagreInntekter(fødselsnummer, skjæringstidspunkt, inntekter)
    }

    override fun upsertInfotrygdutbetalinger(
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ) = sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        TransactionalPersonDao(session).upsertInfotrygdutbetalinger(fødselsnummer, utbetalinger)
    }

    internal fun insertPerson(
        fødselsnummer: String,
        aktørId: String,
        personinfoId: Long,
        enhetId: Int,
        infotrygdutbetalingerId: Long,
    ) = requireNotNull(
        sessionOf(dataSource, returnGeneratedKey = true).use { session ->
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
                        "timestamp" to LocalDateTime.now(),
                    ),
                ).asUpdateAndReturnGeneratedKey,
            )
        },
    )

    override fun finnEnhetId(fødselsnummer: String) =
        sessionOf(dataSource).use { session ->
            TransactionalPersonDao(session).finnEnhetId(fødselsnummer)
        }
}

internal fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
