package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.db.MedDataSource
import no.nav.helse.db.MedSession
import no.nav.helse.db.PersonRepository
import no.nav.helse.db.QueryRunner
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

internal class PersonDao(
    queryRunner: QueryRunner,
) : PersonRepository, QueryRunner by queryRunner {
    constructor(session: Session) : this(MedSession(session))
    constructor(dataSource: DataSource) : this(MedDataSource(dataSource))

    override fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto? =
        finnPerson(fødselsnummer)?.let {
            MinimalPersonDto(
                fødselsnummer = it.fødselsnummer,
                aktørId = it.aktørId,
            )
        }

    override fun lagreMinimalPerson(minimalPerson: MinimalPersonDto) {
        asSQL(
            """INSERT INTO person (fodselsnummer, aktor_id) VALUES (:foedselsnummer, :aktoerId)""",
            "foedselsnummer" to minimalPerson.fødselsnummer.toLong(),
            "aktoerId" to minimalPerson.aktørId.toLong(),
        ).update()
    }

    override fun personKlargjort(fødselsnummer: String) {
        asSQL("DELETE FROM person_klargjores WHERE fødselsnummer = :foedselsnummer", "foedselsnummer" to fødselsnummer).update()
    }

    internal fun finnPerson(fødselsnummer: String) =
        asSQL(
            "SELECT aktor_id, fodselsnummer FROM person WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single { row ->
            PersonDto(
                aktørId = row.long("aktor_id").toString(),
                fødselsnummer = row.long("fodselsnummer").toFødselsnummer(),
                vedtaksperioder = emptyList(),
                avviksvurderinger = emptyList(),
                skjønnsfastsatteSykepengegrunnlag = emptyList(),
            )
        }

    override fun finnPersonMedFødselsnummer(fødselsnummer: String): Long? =
        asSQL(
            "SELECT id FROM person WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single { row -> row.long("id") }

    internal fun finnAktørId(fødselsnummer: String): String? =
        asSQL(
            "SELECT aktor_id FROM person WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single { it.string("aktor_id") }

    override fun finnPersoninfoSistOppdatert(fødselsnummer: String) =
        asSQL(
            "SELECT personinfo_oppdatert FROM person WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single { row -> row.localDateOrNull("personinfo_oppdatert") }

    override fun finnPersoninfoRef(fødselsnummer: String) =
        asSQL(
            "SELECT info_ref FROM person WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single { row -> row.longOrNull("info_ref") }

    private fun insertPersoninfo(
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
        fødselsnummer: String,
    ) {
        val personinfoId =
            asSQL(
                """
                INSERT INTO person_info(fornavn, mellomnavn, etternavn, fodselsdato, kjonn, adressebeskyttelse)
                VALUES(:fornavn, :mellomnavn, :etternavn, :foedselsdato, CAST(:kjonn as person_kjonn), :adressebeskyttelse);
                """.trimIndent(),
                "fornavn" to fornavn,
                "mellomnavn" to mellomnavn,
                "etternavn" to etternavn,
                "foedselsdato" to fødselsdato,
                "kjonn" to kjønn.name,
                "adressebeskyttelse" to adressebeskyttelse.name,
            ).updateAndReturnGeneratedKey()
        checkNotNull(personinfoId)
        updatePersoninfoRef(personinfoId, fødselsnummer)
    }

    private fun updatePersoninfoRef(
        id: Long,
        fødselsnummer: String,
    ) {
        asSQL(
            "UPDATE person SET info_ref = :id, personinfo_oppdatert = now() WHERE fodselsnummer = :foedselsnummer",
            "id" to id,
            "foedselsnummer" to fødselsnummer.toLong(),
        ).update()
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
        finnPersonInfoRef(fødselsnummer)
            ?.also {
                updatePersonInfo(
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
            ?: insertPersoninfo(
                fornavn,
                mellomnavn,
                etternavn,
                fødselsdato,
                kjønn,
                adressebeskyttelse,
                fødselsnummer,
            )
    }

    private fun finnPersonInfoRef(fødselsnummer: String) =
        asSQL(
            "SELECT info_ref FROM person WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single { it.longOrNull("info_ref") }

    private fun updatePersonInfo(
        id: Long,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
        fødselsnummer: String,
    ) {
        asSQL(
            """
            UPDATE person_info 
            SET fornavn = :fornavn, 
                mellomnavn = :mellomnavn, 
                etternavn = :etternavn, 
                fodselsdato = :foedselsdato, 
                kjonn = CAST(:kjonn as person_kjonn), 
                adressebeskyttelse = :adressebeskyttelse 
            WHERE id = :id
            """.trimIndent(),
            "fornavn" to fornavn,
            "mellomnavn" to mellomnavn,
            "etternavn" to etternavn,
            "foedselsdato" to fødselsdato,
            "kjonn" to kjønn.name,
            "adressebeskyttelse" to adressebeskyttelse.name,
            "id" to id,
        ).update()

        updatePersoninfoOppdatert(fødselsnummer)
    }

    private fun updatePersoninfoOppdatert(fødselsnummer: String) {
        asSQL(
            "UPDATE person SET personinfo_oppdatert = now() WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).update()
    }

    override fun finnAdressebeskyttelse(fødselsnummer: String) =
        asSQL(
            """
            SELECT adressebeskyttelse FROM person_info
            WHERE id = (SELECT info_ref FROM person WHERE fodselsnummer = :foedselsnummer);
            """.trimIndent(),
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single { row -> Adressebeskyttelse.valueOf(row.string("adressebeskyttelse")) }

    override fun finnEnhetSistOppdatert(fødselsnummer: String) =
        asSQL("SELECT enhet_ref_oppdatert FROM person WHERE fodselsnummer = :foedselsnummer", "foedselsnummer" to fødselsnummer.toLong())
            .single { row -> row.localDateOrNull("enhet_ref_oppdatert") }

    override fun oppdaterEnhet(
        fødselsnummer: String,
        enhetNr: Int,
    ) = asSQL(
        "UPDATE person SET enhet_ref = :enhetNr, enhet_ref_oppdatert = now() WHERE fodselsnummer = :foedselsnummer",
        "enhetNr" to enhetNr,
        "foedselsnummer" to fødselsnummer.toLong(),
    ).update()

    override fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String) =
        asSQL(
            "SELECT infotrygdutbetalinger_oppdatert FROM person WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single { it.localDateOrNull("infotrygdutbetalinger_oppdatert") }

    override fun finnInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
    ) = asSQL(
        """
        SELECT * FROM inntekt 
        WHERE person_ref = (SELECT id FROM person WHERE fodselsnummer = :foedselsnummer)
        AND skjaeringstidspunkt = :skjaeringstidspunkt;
        """.trimIndent(),
        "foedselsnummer" to fødselsnummer.toLong(),
        "skjaeringstidspunkt" to skjæringstidspunkt,
    ).single { objectMapper.readValue<List<Inntekter>>(it.string("inntekter")) }

    override fun lagreInntekter(
        fødselsnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekter: List<Inntekter>,
    ) = finnPersonRef(fødselsnummer)?.also {
        asSQL(
            """
            INSERT INTO inntekt (person_ref, skjaeringstidspunkt, inntekter)
            VALUES (:person_ref, :skjaeringstidspunkt, :inntekter::json)
            """.trimIndent(),
            "person_ref" to it,
            "skjaeringstidspunkt" to skjæringstidspunkt,
            "inntekter" to objectMapper.writeValueAsString(inntekter),
        ).update()
    }

    private fun finnPersonRef(fødselsnummer: String) =
        asSQL(
            "SELECT id FROM person WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single { it.longOrNull("id") }

    override fun upsertInfotrygdutbetalinger(
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ) = finnInfotrygdutbetalingerRef(fødselsnummer)
        ?.also {
            updateInfotrygdutbetalinger(it, fødselsnummer, utbetalinger)
        }
        ?: insertInfotrygdubetalinger(utbetalinger, fødselsnummer)

    private fun updateInfotrygdutbetalinger(
        infotrygdutbetalingerId: Long,
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ) {
        asSQL(
            """
            UPDATE infotrygdutbetalinger SET data = CAST(:utbetalinger as json)
            WHERE id = :infotrygdutbetalingerId;
            """.trimIndent(),
            "utbetalinger" to objectMapper.writeValueAsString(utbetalinger),
            "infotrygdutbetalingerId" to infotrygdutbetalingerId,
        ).update()

        updateInfotrygdutbetalingerOppdatert(fødselsnummer)
    }

    private fun updateInfotrygdutbetalingerOppdatert(fødselsnummer: String) {
        asSQL(
            "UPDATE person SET infotrygdutbetalinger_oppdatert = now() WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).update()
    }

    private fun insertInfotrygdubetalinger(
        utbetalinger: JsonNode,
        fødselsnummer: String,
    ): Long {
        val infotrygdutbetalingerId =
            asSQL(
                "INSERT INTO infotrygdutbetalinger (data) VALUES (CAST(:data as json))",
                "data" to objectMapper.writeValueAsString(utbetalinger),
            ).updateAndReturnGeneratedKey()
        checkNotNull(infotrygdutbetalingerId)
        updateInfotrygdutbetalingerRef(infotrygdutbetalingerId, fødselsnummer)
        return infotrygdutbetalingerId
    }

    private fun updateInfotrygdutbetalingerRef(
        infotrygdutbetalingerId: Long,
        fødselsnummer: String,
    ) {
        asSQL(
            """
            UPDATE person SET infotrygdutbetalinger_ref = :infotrygdutbetalingerId, infotrygdutbetalinger_oppdatert = now()
            WHERE fodselsnummer = :foedselsnummer
            """.trimIndent(),
            "infotrygdutbetalingerId" to infotrygdutbetalingerId,
            "foedselsnummer" to fødselsnummer.toLong(),
        ).update()
    }

    private fun finnInfotrygdutbetalingerRef(fødselsnummer: String) =
        asSQL(
            "SELECT infotrygdutbetalinger_ref FROM person WHERE fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single { it.longOrNull("infotrygdutbetalinger_ref") }

    internal fun insertPerson(
        fødselsnummer: String,
        aktørId: String,
        personinfoId: Long,
        enhetId: Int,
        infotrygdutbetalingerId: Long,
    ) = asSQL(
        """
        INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref, enhet_ref_oppdatert, personinfo_oppdatert, infotrygdutbetalinger_oppdatert)
        VALUES(:foedselsnummer, :aktorId, :personinfoId, :enhetId, :infotrygdutbetalingerId, :timestamp, :timestamp, :timestamp);
        """.trimIndent(),
        "foedselsnummer" to fødselsnummer.toLong(),
        "aktorId" to aktørId.toLong(),
        "personinfoId" to personinfoId,
        "enhetId" to enhetId,
        "infotrygdutbetalingerId" to infotrygdutbetalingerId,
        "timestamp" to LocalDateTime.now(),
    ).updateAndReturnGeneratedKey().let(::checkNotNull)

    override fun finnEnhetId(fødselsnummer: String): String =
        asSQL(
            "SELECT enhet_ref FROM person where fodselsnummer = :foedselsnummer",
            "foedselsnummer" to fødselsnummer.toLong(),
        ).single {
            it.int("enhet_ref").toEnhetnummer()
        }.let { checkNotNull(it) }
}

private fun Int.toEnhetnummer() = if (this < 1000) "0$this" else "$this"

internal fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else "$this"
