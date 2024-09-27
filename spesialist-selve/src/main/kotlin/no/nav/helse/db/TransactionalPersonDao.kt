package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.mediator.meldinger.løsninger.Inntekter
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.person.PersonDto
import no.nav.helse.modell.person.toFødselsnummer
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

    override fun finnEnhetSistOppdatert(fødselsnummer: String): LocalDate = throw OperationNotSupportedException()

    override fun oppdaterEnhet(
        fødselsnummer: String,
        enhetNr: Int,
    ): Int = throw OperationNotSupportedException()

    override fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String): LocalDate = throw OperationNotSupportedException()

    override fun upsertInfotrygdutbetalinger(
        fødselsnummer: String,
        utbetalinger: JsonNode,
    ): Long = throw OperationNotSupportedException()

    override fun upsertPersoninfo(
        fødselsnummer: String,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse,
    ) {
        throw OperationNotSupportedException()
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

    override fun finnPersonMedFødselsnummer(fødselsnummer: String): Long? = throw OperationNotSupportedException()

    override fun finnPersoninfoRef(fødselsnummer: String): Long? = throw OperationNotSupportedException()

    override fun finnPersoninfoSistOppdatert(fødselsnummer: String): LocalDate = throw OperationNotSupportedException()

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
}
