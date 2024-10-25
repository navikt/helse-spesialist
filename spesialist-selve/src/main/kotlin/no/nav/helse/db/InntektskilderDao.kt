package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQLWithQuestionMarks
import no.nav.helse.HelseDao.Companion.list
import no.nav.helse.modell.InntektskildeDto
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.NyInntektskildeDto

internal class InntektskilderDao(
    private val session: Session,
) : InntektskilderRepository {
    private val arbeidsgiverDao = TransactionalArbeidsgiverDao(session)
    private val avviksvurderingDao = TransactionalAvviksvurderingDao(session)

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
    ): List<InntektskildeDto> {
        val alleOrganisasjonsnumre =
            andreOrganisasjonsnumre + organisasjonsnumreFraSammenligningsgrunnlag(fødselsnummer).distinct()
        val eksisterendeInntektskilder = eksisterendeInntektskilder(alleOrganisasjonsnumre)
        val nyeInntektskilder = andreOrganisasjonsnumre.organisasjonsnumreSomIkkeFinnesI(eksisterendeInntektskilder)
        return eksisterendeInntektskilder + nyeInntektskilder
    }

    private fun List<String>.organisasjonsnumreSomIkkeFinnesI(inntektskilder: List<InntektskildeDto>) =
        filterNot { organisasjonsnummer -> organisasjonsnummer in inntektskilder.map { it.organisasjonsnummer } }
            .map { NyInntektskildeDto(it, inntektskildetype(it)) }

    private fun eksisterendeInntektskilder(organisasjonsnumre: List<String>): List<InntektskildeDto> {
        if (organisasjonsnumre.isEmpty()) return emptyList()
        return asSQLWithQuestionMarks(
            """
                SELECT orgnummer, navn, bransjer, an.navn_oppdatert FROM arbeidsgiver ag
                INNER JOIN arbeidsgiver_navn an on an.id = ag.navn_ref
                LEFT JOIN arbeidsgiver_bransjer ab on ab.id = ag.bransjer_ref
                WHERE orgnummer = ANY (?)
            """,
            organisasjonsnumre.map { it.toLong() }.toTypedArray(),
        ).list(session) {
            val organisasjonsnummer = it.string("orgnummer")
            KomplettInntektskildeDto(
                organisasjonsnummer = organisasjonsnummer,
                type = inntektskildetype(organisasjonsnummer),
                navn = it.string("navn"),
                bransjer =
                    it
                        .stringOrNull("bransjer")
                        ?.removeSurrounding("[", "]")
                        ?.replace("\"", "")
                        ?.split(",")
                        ?.toList() ?: emptyList(),
                sistOppdatert = it.localDate("navn_oppdatert"),
            )
        }
    }

    private fun organisasjonsnumreFraSammenligningsgrunnlag(fødselsnummer: String): List<String> =
        avviksvurderingDao
            .finnAvviksvurderinger(fødselsnummer)
            .flatMap { it.sammenligningsgrunnlag.innrapporterteInntekter }
            .map { it.arbeidsgiverreferanse }

    private fun inntektskildetype(organisasjonsnummer: String): InntektskildetypeDto =
        when {
            organisasjonsnummer.length == 9 -> InntektskildetypeDto.ORDINÆR
            else -> InntektskildetypeDto.ENKELTPERSONFORETAK
        }
}
