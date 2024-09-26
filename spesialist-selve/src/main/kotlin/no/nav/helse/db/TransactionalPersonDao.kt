package no.nav.helse.db

import kotliquery.TransactionalSession
import kotliquery.queryOf
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.modell.person.PersonDto
import no.nav.helse.modell.person.toFødselsnummer
import org.intellij.lang.annotations.Language

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
