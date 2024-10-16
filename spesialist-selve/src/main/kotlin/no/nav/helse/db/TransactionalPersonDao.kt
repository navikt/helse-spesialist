package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.Session
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

internal class TransactionalPersonDao(
    private val session: Session,
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
        session.run(queryOf(query, mapOf("fodselsnummer" to fødselsnummer)).asUpdate)
    }

    override fun finnEnhetSistOppdatert(fødselsnummer: String): LocalDate? {
        @Language("PostgreSQL")
        val query = "SELECT enhet_ref_oppdatert FROM person WHERE fodselsnummer=?;"
        return session.run(
            queryOf(query, fødselsnummer.toLong())
                .map { row ->
                    row.localDateOrNull("enhet_ref_oppdatert")
                }.asSingle,
        )
    }

    override fun oppdaterEnhet(
        fødselsnummer: String,
        enhetNr: Int,
    ): Int {
        @Language("PostgreSQL")
        val query =
            "UPDATE person SET enhet_ref=:enhetNr, enhet_ref_oppdatert=now() WHERE fodselsnummer=:fodselsnummer;"
        return session.run(
            queryOf(
                query,
                mapOf(
                    "enhetNr" to enhetNr,
                    "fodselsnummer" to fødselsnummer.toLong(),
                ),
            ).asUpdate,
        )
    }

    override fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String): LocalDate = throw UnsupportedOperationException()

    override fun upsertInfotrygdutbetalinger(
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ): Long {
        return session
            .finnInfotrygdutbetalingerRef(fødselsnummer)
            ?.also {
                session.updateInfotrygdutbetalinger(it, fødselsnummer, utbetalinger)
            }
            ?: session.insertInfotrygdubetalinger(utbetalinger, fødselsnummer)
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
        session
            .finnPersonInfoRef(fødselsnummer)
            ?.also {
                session.updatePersonInfo(
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
            ?: session.insertPersoninfo(
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
        throw UnsupportedOperationException()
    }

    override fun lagreInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ): Long? {
        throw UnsupportedOperationException()
    }

    override fun finnEnhetId(fødselsnummer: String): String {
        @Language("PostgreSQL")
        val statement = "SELECT enhet_ref FROM person where fodselsnummer = ?;"
        return requireNotNull(
            session.run(
                queryOf(statement, fødselsnummer.toLong())
                    .map {
                        it.int("enhet_ref").toEnhetnummer()
                    }.asSingle,
            ),
        )
    }

    override fun finnAdressebeskyttelse(fødselsnummer: String): Adressebeskyttelse? {
        @Language("PostgreSQL")
        val adressebeskyttelseQuery = """
            SELECT adressebeskyttelse FROM person_info
            WHERE id=(SELECT info_ref FROM person WHERE fodselsnummer=?);
        """
        return session.run(
            queryOf(adressebeskyttelseQuery, fødselsnummer.toLong())
                .map { row -> Adressebeskyttelse.valueOf(row.string("adressebeskyttelse")) }
                .asSingle,
        )
    }

    override fun finnPersonMedFødselsnummer(fødselsnummer: String): Long = throw UnsupportedOperationException()

    override fun finnPersoninfoRef(fødselsnummer: String): Long = throw UnsupportedOperationException()

    override fun finnPersoninfoSistOppdatert(fødselsnummer: String): LocalDate? {
        @Language("PostgreSQL")
        val query = "SELECT personinfo_oppdatert FROM person WHERE fodselsnummer=?;"
        return session.run(
            queryOf(query, fødselsnummer.toLong())
                .map { row -> row.localDateOrNull("personinfo_oppdatert") }
                .asSingle,
        )
    }

    override fun lagreMinimalPerson(minimalPerson: MinimalPersonDto) {
        @Language("PostgreSQL")
        val query = """INSERT INTO person(fodselsnummer, aktor_id) VALUES(:fodselsnummer, :aktorId); """
        session.run(
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
        return session.run(
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

    private fun Session.finnInfotrygdutbetalingerRef(fødselsnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer=?"

        return run(queryOf(query, fødselsnummer.toLong()).map { it.longOrNull("infotrygdutbetalinger_ref") }.asSingle)
    }

    private fun Session.updateInfotrygdutbetalinger(
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

    private fun Session.updateInfotrygdutbetalingerOppdatert(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "UPDATE person SET infotrygdutbetalinger_oppdatert=now() WHERE fodselsnummer=?"

        run(queryOf(query, fødselsnummer.toLong()).asUpdate)
    }

    private fun Session.insertInfotrygdubetalinger(
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

    private fun Session.updateInfotrygdutbetalingerRef(
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

    private fun Session.finnPersonInfoRef(fødselsnummer: String): Long? {
        @Language("PostgreSQL")
        val query = "SELECT info_ref FROM person WHERE fodselsnummer=?"

        return run(queryOf(query, fødselsnummer.toLong()).map { it.longOrNull("info_ref") }.asSingle)
    }

    private fun Session.updatePersonInfo(
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

    private fun Session.updatePersoninfoOppdatert(fødselsnummer: String) {
        @Language("PostgreSQL")
        val query = "UPDATE person SET personinfo_oppdatert=now() WHERE fodselsnummer=?"

        run(queryOf(query, fødselsnummer.toLong()).asUpdate)
    }

    private fun Session.insertPersoninfo(
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

    private fun Session.updatePersoninfoRef(
        id: Long,
        fødselsnummer: String,
    ) {
        @Language("PostgreSQL")
        val query = "UPDATE person SET info_ref=:id, personinfo_oppdatert=now() WHERE fodselsnummer=:fodselsnummer"

        run(queryOf(query, mapOf("id" to id, "fodselsnummer" to fødselsnummer.toLong())).asUpdate)
    }
}

private fun Int.toEnhetnummer() = if (this < 1000) "0$this" else this.toString()
