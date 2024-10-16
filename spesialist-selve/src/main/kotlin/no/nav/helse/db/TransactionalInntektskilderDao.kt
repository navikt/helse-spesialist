package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.modell.InntektskildeDto
import no.nav.helse.modell.KomplettInntektskildeDto

internal class TransactionalInntektskilderDao(
    session: Session,
) : InntektskilderRepository {
    private val arbeidsgiverDao = TransactionalArbeidsgiverDao(session)

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
    ): List<InntektskildeDto> = throw UnsupportedOperationException()
}
