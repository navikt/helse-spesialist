package no.nav.helse.modell.arbeidsforhold

import kotliquery.sessionOf
import no.nav.helse.db.ArbeidsforholdRepository
import no.nav.helse.db.TransactionalArbeidsforholdDao
import no.nav.helse.modell.KomplettArbeidsforholdDto
import javax.sql.DataSource

class ArbeidsforholdDao(private val dataSource: DataSource) : ArbeidsforholdRepository {
    override fun upsertArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
        arbeidsforhold: List<KomplettArbeidsforholdDto>,
    ) = sessionOf(dataSource).use {
        TransactionalArbeidsforholdDao(it).upsertArbeidsforhold(fødselsnummer, organisasjonsnummer, arbeidsforhold)
    }

    override fun findArbeidsforhold(
        fødselsnummer: String,
        organisasjonsnummer: String,
    ): List<KomplettArbeidsforholdDto> {
        sessionOf(dataSource).use { session ->
            return TransactionalArbeidsforholdDao(session).findArbeidsforhold(fødselsnummer, organisasjonsnummer)
        }
    }
}
