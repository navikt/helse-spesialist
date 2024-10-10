package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.person.PersonDto
import no.nav.helse.modell.person.toFødselsnummer
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.typer.Kjønn
import org.intellij.lang.annotations.Language
import java.time.LocalDate
import javax.naming.OperationNotSupportedException

internal class TransactionalPersonDao(
    private val transactionalSession: TransactionalSession,
) : PersonRepository {
    override fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto? =
        finnPerson(fødselsnummer)?.let {
            MinimalPersonDto(
                fødselsnummer = it.fødselsnummer,
                aktørId = it.aktørId,
            )
        }

    override fun personKlargjort(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "DELETE FROM person_klargjores WHERE fødselsnummer = :fodselsnummer"
        transactionalSession.run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer)).asUpdate)
    }

    override fun finnEnhetSistOppdatert(fødselsnummer: String): LocalDate = throw OperationNotSupportedException()

    override fun oppdaterEnhet(
        fødselsnummer: String,
        enhetNr: Int,
    ): Int = throw OperationNotSupportedException()

    override fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String): LocalDate = throw OperationNotSupportedException()

    override fun upsertInfotrygdutbetalinger(
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ): Long {
        return transactionalSession
            .finnInfotrygdutbetalingerRef(fødselsnummer)
            ?.also {
                transactionalSession.updateInfotrygdutbetalinger(it, fødselsnummer, utbetalinger)
            }
            ?: transactionalSession.insertInfotrygdubetalinger(utbetalinger, fødselsnummer)
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
        transactionalSession
            .finnPersonInfoRef(fødselsnummer)
            ?.also {
                transactionalSession.updatePersonInfo(
                    it,
                    fornavn,
                    mellomnavn,
                    etternavn,
                    fødselsdato,
                    kjønn,
                    adressebeskyttelse,
                    fødselsnummer,
                )
            }
            ?: transactionalSession.insertPersoninfo(
                fornavn,
                mellomnavn,
                etternavn,
                fødselsdato,
                kjønn,
                adressebeskyttelse,
                fødselsnummer,
            )
    }

    override fun finnInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ): List<Inntekter>? {
        throw OperationNotSupportedException()
    }

    override fun lagreInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ): Long? {
        throw OperationNotSupportedException()
    }

    override fun finnEnhetId(fødselsnummer: String): String = throw OperationNotSupportedException()

    override fun finnAdressebeskyttelse(fødselsnummer: String): Adressebeskyttelse? {
        @Language("PostgreSQL")
        val adressebeskyttelseQuery = """
            SELECT adressebeskyttelse FROM person_info
            WHERE id=(SELECT info_ref FROM person WHERE fodselsnummer=?);
        """
        return transactionalSession.run(
            queryOf(adressebeskyttelseQuery, fødselsnummer.toLong())
                .map { row -> Adressebeskyttelse.valueOf(row.string("adressebeskyttelse")) }
                .asSingle,
        )
    }

    override fun finnPersonMedFødselsnummer(fødselsnummer: String): Long = throw OperationNotSupportedException()

    override fun finnPersoninfoRef(fødselsnummer: String): Long = throw OperationNotSupportedException()

    override fun finnPersoninfoSistOppdatert(fødselsnummer: String): LocalDate? {
        @Language("PostgreSQL")
        val query = "SELECT personinfo_oppdatert FROM person WHERE fodselsnummer=?;"
        return transactionalSession.run(
            queryOf(query, fødselsnummer.toLong())
                .map { row -> row.localDateOrNull("personinfo_oppdatert") }
                .asSingle,
        )
    }

    override fun lagreMinimalPerson(minimalPerson: MinimalPersonDto) {
        @Language("PostgreSQL")
        val query = """INSERT INTO person(fodselsnummer, aktor_id) VALUES(:fodselsnummer, :aktorId); """
        transactionalSession.run(
            queryOf(
                query,
                mapOf(
                    "fodselsnummer" to minimalPerson.fødselsnummer.toLong(),
                    "aktorId" to minimalPerson.aktørId.toLong(),
                ),
            ).asUpdate,
        )
    }

    internal fun finnPerson(fødselsnummer: String): PersonDto? {
        @Language("PostgreSQL")
        val query = "SELECT aktor_id, fodselsnummer FROM person WHERE fodselsnummer = :fodselsnummer"
        return transactionalSession.run(
            queryOf(
                query,
                mapOf("fodselsnummer" to fødselsnummer.toLong()),
            ).map { row ->
                PersonDto(
                    aktørId = row.long("aktor_id").toString(),
                    fødselsnummer = row.long("fodselsnummer").toFødselsnummer(),
                    vedtaksperioder = emptyList(),
                    avviksvurderinger = emptyList(),
                    skjønnsfastsatteSykepengegrunnlag = emptyList(),
                )
            }.asSingle,
        )
    }

    private fun TransactionalSession.finnInfotrygdutbetalingerRef(fødselsnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer=?"

        return run(queryOf(query, fødselsnummer.toLong()).map { it.longOrNull("infotrygdutbetalinger_ref") }.asSingle)
    }

    private fun TransactionalSession.updateInfotrygdutbetalinger(
        infotrygdutbetalingerId: Long,
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ) {
        @Language("PostgreSQL")
        val query =
            """
            UPDATE infotrygdutbetalinger SET data=CAST(:utbetalinger as json)
            WHERE id=:infotrygdutbetalingerId;
            """.trimIndent()
        run(
            queryOf(
                query,
                mapOf(
                    "utbetalinger" to objectMapper.writeValueAsString(utbetalinger),
                    "infotrygdutbetalingerId" to infotrygdutbetalingerId,
                ),
            ).asUpdate,
        )
        updateInfotrygdutbetalingerOppdatert(fødselsnummer)
    }

    private fun TransactionalSession.updateInfotrygdutbetalingerOppdatert(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "UPDATE person SET infotrygdutbetalinger_oppdatert=now() WHERE fodselsnummer=?"

        run(queryOf(query, fødselsnummer.toLong()).asUpdate)
    }

    private fun TransactionalSession.insertInfotrygdubetalinger(
        utbetalinger: JsonNode,
        fødselsnummer: String,
    ): Long {
        @Language("PostgreSQL")
        val query = "INSERT INTO infotrygdutbetalinger (data) VALUES (CAST(? as json))"

        val infotrygdutbetalingerId =
            requireNotNull(
                run(
                    queryOf(
                        query,
                        objectMapper.writeValueAsString(utbetalinger),
                    ).asUpdateAndReturnGeneratedKey,
                ),
            )
        updateInfotrygdutbetalingerRef(infotrygdutbetalingerId, fødselsnummer)
        return infotrygdutbetalingerId
    }

    private fun TransactionalSession.updateInfotrygdutbetalingerRef(
        infotrygdutbetalingerId: Long,
        fødselsnummer: String,
    ) {
        @Language("PostgreSQL")
        val query =
            """
            UPDATE person SET infotrygdutbetalinger_ref=:infotrygdutbetalingerId, infotrygdutbetalinger_oppdatert=now()
            WHERE fodselsnummer=:fodselsnummer;
            """.trimIndent()
        run(
            queryOf(
                query,
                mapOf(
                    "infotrygdutbetalingerId" to infotrygdutbetalingerId,
                    "fodselsnummer" to fødselsnummer.toLong(),
                ),
            ).asUpdate,
        )
    }

    private fun TransactionalSession.finnPersonInfoRef(fødselsnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT info_ref FROM person WHERE fodselsnummer=?"

        return run(queryOf(query, fødselsnummer.toLong()).map { it.longOrNull("info_ref") }.asSingle)
    }

    private fun TransactionalSession.updatePersonInfo(
        id: Long,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
        fødselsnummer: String,
    ) {
        @Language("PostgreSQL")
        val query =
            """
            UPDATE person_info 
            SET fornavn=:fornavn, 
                mellomnavn=:mellomnavn, 
                etternavn=:etternavn, 
                fodselsdato=:fodselsdato, 
                kjonn=CAST(:kjonn as person_kjonn), 
                adressebeskyttelse=:adressebeskyttelse 
            WHERE id=:id
            """.trimIndent()

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
                    "id" to id,
                ),
            ).asUpdate,
        )
        updatePersoninfoOppdatert(fødselsnummer)
    }

    private fun TransactionalSession.updatePersoninfoOppdatert(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "UPDATE person SET personinfo_oppdatert=now() WHERE fodselsnummer=?"

        run(queryOf(query, fødselsnummer.toLong()).asUpdate)
    }

    private fun TransactionalSession.insertPersoninfo(
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
        fødselsnummer: String,
    ) {
        @Language("PostgreSQL")
        val query = """
            INSERT INTO person_info(fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
            VALUES(:fornavn, :mellomnavn, :etternavn, :fodselsdato, CAST(:kjonn as person_kjonn), :adressebeskyttelse);
        """

        val personinfoId =
            requireNotNull(
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
                        ),
                    ).asUpdateAndReturnGeneratedKey,
                ),
            )
        updatePersoninfoRef(personinfoId, fødselsnummer)
    }

    private fun TransactionalSession.updatePersoninfoRef(
        id: Long,
        fødselsnummer: String,
    ) {
        @Language("PostgreSQL")
        val query = "UPDATE person SET info_ref=:id, personinfo_oppdatert=now() WHERE fodselsnummer=:fodselsnummer"

        run(queryOf(query, mapOf("id" to id, "fodselsnummer" to fødselsnummer.toLong())).asUpdate)
    }
}
