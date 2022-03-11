package no.nav.helse.modell.person

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.modell.person.PersonDao.Utbetalingen.Companion.somUtbetaling
import no.nav.helse.objectMapper
import no.nav.helse.person.Adressebeskyttelse
import no.nav.helse.person.Kjønn
import no.nav.helse.person.SnapshotDto
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.isMissingOrNull
import org.intellij.lang.annotations.Language

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

    internal fun updatePersoninfo(
        fødselsnummer: String,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse
    ) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val personinfoQuery = """
            UPDATE person_info SET fornavn=:fornavn, mellomnavn=:mellomnavn, etternavn=:etternavn, fodselsdato=:fodselsdato, kjonn=CAST(:kjonn as person_kjonn), adressebeskyttelse=:adressebeskyttelse
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
                    "fodselsnummer" to fødselsnummer.toLong(),
                    "adressebeskyttelse" to adressebeskyttelse.name
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

    internal fun findPersoninfoAdressebeskyttelse(fødselsnummer: String) = sessionOf(dataSource).use { session ->
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
        requireNotNull(
            session.run(
                queryOf(query, fødselsnummer.toLong()).map { row ->
                    row.sqlDate("enhet_ref_oppdatert").toLocalDate()
                }.asSingle
            )
        )
    }

    internal fun findVedtaksperiodeUtbetalingElement(fødselsnummer: String, utbetalingId: UUID) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val query = """
            SELECT * FROM person AS p
            INNER JOIN speil_snapshot AS ss ON ss.person_ref = p.id
            WHERE p.fodselsnummer = ?;
        """
        session.run(
            queryOf(query, fødselsnummer.toLong()).map { row ->
                objectMapper.readValue<SnapshotDto>(row.string("data")).arbeidsgivere.flatMap { arbeidsgiver ->
                    arbeidsgiver.vedtaksperioder.map { vedtaksperiode ->
                        vedtaksperiode["utbetaling"]?.somUtbetaling()
                    }
                }.firstOrNull { it?.utbetalingId == utbetalingId }
            }.asSingle
        )

    }
    data class Utbetalingen(
        val utbetalingId: UUID?,
        val utbetalingstidslinje: List<Utbetalingstidslinjedag>
    ) {
        data class Utbetalingstidslinjedag(
            val dato: LocalDate,
            val personbeløp: Int?,
            val arbeidsgiverbeløp: Int?
        ) {
            val harPersonutbetaling = personbeløp != null && personbeløp > 0
            val harArbeidsgiverutbetaling = arbeidsgiverbeløp != null && arbeidsgiverbeløp > 0
            val ingenArbeidsgiverutbetaling = arbeidsgiverbeløp == null || arbeidsgiverbeløp == 0
            val ingenPersonutbetaling = personbeløp == null || personbeløp == 0
            val delvisRefusjon = harPersonutbetaling && harArbeidsgiverutbetaling
        }

        val harPersonutbetaling = utbetalingstidslinje.any { dag -> dag.harPersonutbetaling }
        val barePersonutbetaling = harPersonutbetaling && utbetalingstidslinje.all { dag -> dag.ingenArbeidsgiverutbetaling }
        val harArbeidsgiverutbetaling = utbetalingstidslinje.any { dag -> dag.harArbeidsgiverutbetaling }
        val bareArbeidsgiverutbetaling = utbetalingstidslinje.all { dag -> dag.ingenPersonutbetaling }
        val overlappendeUtbetaling = utbetalingstidslinje.any { dag -> dag.delvisRefusjon }
        val delvisRefusjon = overlappendeUtbetaling

        internal fun utbetalingTilSykmeldt(fom: LocalDate, tom: LocalDate) = utbetalingstidslinje.filter { it.dato in fom..tom }.any { it.harPersonutbetaling }
        internal fun barePersonutbetaling(fom: LocalDate, tom: LocalDate) = utbetalingstidslinje.filter { it.dato in fom..tom }.let {
            it.any { it.harPersonutbetaling } && it.all { it.ingenArbeidsgiverutbetaling }
        }
        internal fun bareArbeidsgiverutbetaling(fom: LocalDate, tom: LocalDate) = utbetalingstidslinje.filter { it.dato in fom..tom }.let {
            it.any { it.harArbeidsgiverutbetaling } && it.all { it.ingenPersonutbetaling }
        }
        internal fun delvisRefusjon(fom: LocalDate, tom: LocalDate) = utbetalingstidslinje.filter { it.dato in fom..tom }.any { it.delvisRefusjon }

        internal companion object {
            internal fun Utbetalingen?.utbetalingTilSykmeldt(fom: LocalDate, tom: LocalDate) = this?.utbetalingTilSykmeldt(fom, tom) == true
            internal fun Utbetalingen?.barePersonutbetaling(fom: LocalDate, tom: LocalDate) = this?.barePersonutbetaling(fom, tom) == true
            internal fun Utbetalingen?.bareArbeidsgiverutbetaling(fom: LocalDate, tom: LocalDate) = this?.bareArbeidsgiverutbetaling(fom, tom) == true
            internal fun Utbetalingen?.delvisRefusjon(fom: LocalDate, tom: LocalDate) = this?.delvisRefusjon(fom, tom) == true

            private fun JsonNode.getOrNull(felt: String) = path(felt).takeUnless { it.isMissingOrNull() }
            private fun oppdragsperiode(oppdrag: JsonNode): ClosedRange<LocalDate>? {
                val min = oppdrag.path("utbetalingslinjer").minOfOrNull { linje ->
                    linje.path("fom").asLocalDate()
                } ?: return null
                val max = oppdrag.path("utbetalingslinjer").maxOfOrNull { linje ->
                    linje.path("tom").asLocalDate()
                } ?: return null
                return min..max
            }
            private fun utbetalingstidslinje(tidslinje: JsonNode, periode: ClosedRange<LocalDate>?): List<JsonNode> {
                if (periode == null) return emptyList()
                return tidslinje.filter { dag ->  dag.path("dato").asLocalDate() in periode }
            }
            internal fun JsonNode.somUtbetaling(): Utbetalingen {
                val arbeidsgiverUtbetalingsperiode = oppdragsperiode( path("arbeidsgiverOppdrag"))
                val personUtbetalingsperiode = oppdragsperiode( path("personOppdrag"))
                val førstedato = listOfNotNull(arbeidsgiverUtbetalingsperiode?.start, personUtbetalingsperiode?.start).minOrNull()
                val sistedato = listOfNotNull(arbeidsgiverUtbetalingsperiode?.endInclusive, personUtbetalingsperiode?.endInclusive).maxOrNull()
                val periode = førstedato?.let { først -> sistedato?.let { sist -> først..sist } }
                val utbetalingstidslinje = utbetalingstidslinje(path("utbetalingstidslinje"), periode) // fjerner alle dager som ikke er del av oppdragene
                return Utbetalingen(
                    utbetalingId = getOrNull("utbetalingId")?.let { UUID.fromString(it.asText()) },
                    utbetalingstidslinje = utbetalingstidslinje.map { dag ->
                        Utbetalingstidslinjedag(
                            dato = dag.path("dato").asLocalDate(),
                            personbeløp = dag.path("personbeløp").takeUnless { it.isMissingOrNull() }?.asInt(),
                            arbeidsgiverbeløp = dag.path("arbeidsgiverbeløp").takeUnless { it.isMissingOrNull() }?.asInt()
                        )
                    }
                )
            }
        }
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
        personinfoId: Long,
        enhetId: Int,
        infotrygdutbetalingerId: Long
    ) = requireNotNull(sessionOf(dataSource, returnGeneratedKey = true).use { session ->
        @Language("PostgreSQL")
        val query = """
            INSERT INTO person(fodselsnummer, aktor_id, info_ref, enhet_ref, infotrygdutbetalinger_ref)
            VALUES(:fodselsnummer, :aktorId, :personinfoId, :enhetId, :infotrygdutbetalingerId);
        """
        session.run(
            queryOf(
                query,
                mapOf(
                    "fodselsnummer" to fødselsnummer.toLong(),
                    "aktorId" to aktørId.toLong(),
                    "personinfoId" to personinfoId,
                    "enhetId" to enhetId,
                    "infotrygdutbetalingerId" to infotrygdutbetalingerId
                )
            ).asUpdateAndReturnGeneratedKey
        )
    })

    internal fun finnEnhetId(fødselsnummer: String) = sessionOf(dataSource).use { session ->
        @Language("PostgreSQL")
        val statement = "SELECT id FROM enhet WHERE id = (SELECT enhet_ref FROM person where fodselsnummer = ?);"
        requireNotNull(session.run(queryOf(statement, fødselsnummer.toLong()).map { it.string("id") }.asSingle))
    }
}

internal fun Long.toFødselsnummer() = if (this < 10000000000) "0$this" else this.toString()
