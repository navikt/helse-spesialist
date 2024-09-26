package no.nav.helse.db

import kotliquery.TransactionalSession
import no.nav.helse.modell.InntektskildeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import javax.naming.OperationNotSupportedException

internal class TransactionalInntektskilderDao(
    transactionalSession: TransactionalSession,
) : InntektskilderRepository {
    private val arbeidsgiverDao = TransactionalArbeidsgiverDao(transactionalSession)

    override fun lagreInntektskilder(inntektskilder: List<InntektskildeDto>) {
        inntektskilder.forEach { inntekt ->
            when (inntekt) {
                is KomplettInntektskildeDto -> {
                    arbeidsgiverDao.upsertNavn(inntekt.organisasjonsnummer, inntekt.navn)
                    arbeidsgiverDao.upsertBransjer(inntekt.organisasjonsnummer, inntekt.bransjer)
                }

                else -> {
                    arbeidsgiverDao.insertMinimalArbeidsgiver(inntekt.organisasjonsnummer)
                }
            }
        }
    }

    override fun inntektskildeEksisterer(orgnummer: String): Boolean = arbeidsgiverDao.findArbeidsgiverByOrgnummer(orgnummer) != null

    override fun finnInntektskilder(
        fødselsnummer: String,
        andreOrganisasjonsnumre: List<String>,
    ): List<InntektskildeDto> = throw OperationNotSupportedException()
}
