package no.nav.helse.db

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.modell.kommando.MinimalPersonDto
import no.nav.helse.spesialist.api.person.Adressebeskyttelse
import no.nav.helse.spesialist.typer.Kjønn
import java.time.LocalDate
import javax.naming.OperationNotSupportedException

abstract class PersonRepositoryMock: PersonRepository {
    override fun finnMinimalPerson(fødselsnummer: String): MinimalPersonDto? = throw OperationNotSupportedException()

    override fun lagreMinimalPerson(minimalPerson: MinimalPersonDto) {
        throw OperationNotSupportedException()
    }

    override fun finnEnhetSistOppdatert(fødselsnummer: String): LocalDate = throw OperationNotSupportedException()

    override fun oppdaterEnhet(fødselsnummer: String, enhetNr: Int): Int = throw OperationNotSupportedException()

    override fun finnITUtbetalingsperioderSistOppdatert(fødselsnummer: String): LocalDate =
        throw OperationNotSupportedException()

    override fun upsertInfotrygdutbetalinger(fødselsnummer: String, utbetalinger: JsonNode): Long =
        throw OperationNotSupportedException()

    override fun upsertPersoninfo(
        fødselsnummer: String,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        fødselsdato: LocalDate,
        kjønn: Kjønn,
        adressebeskyttelse: Adressebeskyttelse
    ) { throw OperationNotSupportedException() }

    override fun finnPersoninfoSistOppdatert(fødselsnummer: String): LocalDate? = throw OperationNotSupportedException()
}