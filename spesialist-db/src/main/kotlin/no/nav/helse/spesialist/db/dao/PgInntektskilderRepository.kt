package no.nav.helse.spesialist.db.dao

import kotliquery.Session
import no.nav.helse.db.ArbeidsgiverDao
import no.nav.helse.db.InntektskilderRepository
import no.nav.helse.modell.InntektskildeDto
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.NyInntektskildeDto
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQLWithQuestionMarks
import no.nav.helse.spesialist.db.HelseDao.Companion.list

class PgInntektskilderRepository internal constructor(
    private val session: Session,
    private val arbeidsgiverDao: ArbeidsgiverDao,
) : InntektskilderRepository {
    private val avviksvurderingRepository = PgAvviksvurderingRepository(session)

    override fun lagreInntektskilder(inntektskilder: List<InntektskildeDto>) {
        inntektskilder.forEach { inntekt ->
            when (inntekt) {
                is KomplettInntektskildeDto -> {
                    arbeidsgiverDao.upsertNavn(inntekt.identifikator, inntekt.navn)
                    arbeidsgiverDao.upsertBransjer(inntekt.identifikator, inntekt.bransjer)
                }

                else -> {
                    arbeidsgiverDao.insertMinimalArbeidsgiver(inntekt.identifikator)
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
            (andreOrganisasjonsnumre + organisasjonsnumreFraSammenligningsgrunnlag(fødselsnummer)).distinct()
        val eksisterendeInntektskilder = eksisterendeInntektskilder(alleOrganisasjonsnumre)
        val nyeInntektskilder = alleOrganisasjonsnumre.organisasjonsnumreSomIkkeFinnesI(eksisterendeInntektskilder)
        return eksisterendeInntektskilder + nyeInntektskilder
    }

    private fun List<String>.organisasjonsnumreSomIkkeFinnesI(inntektskilder: List<InntektskildeDto>) =
        filterNot { organisasjonsnummer -> organisasjonsnummer in inntektskilder.map { it.identifikator } }
            .map { NyInntektskildeDto(it, inntektskildetype(it)) }

    private fun eksisterendeInntektskilder(organisasjonsnumre: List<String>): List<InntektskildeDto> {
        if (organisasjonsnumre.isEmpty()) return emptyList()
        return asSQLWithQuestionMarks(
            """
                SELECT organisasjonsnummer, navn, bransjer, an.navn_oppdatert FROM arbeidsgiver ag
                INNER JOIN arbeidsgiver_navn an on an.id = ag.navn_ref
                LEFT JOIN arbeidsgiver_bransjer ab on ab.id = ag.bransjer_ref
                WHERE organisasjonsnummer = ANY (?)
            """,
            organisasjonsnumre.toTypedArray(),
        ).list(session) {
            val identifikator = it.string("organisasjonsnummer")
            KomplettInntektskildeDto(
                identifikator = identifikator,
                type = inntektskildetype(identifikator),
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
        avviksvurderingRepository
            .finnAvviksvurderinger(fødselsnummer)
            .flatMap { it.sammenligningsgrunnlag.innrapporterteInntekter }
            .map { it.arbeidsgiverreferanse }

    private fun inntektskildetype(organisasjonsnummer: String): InntektskildetypeDto =
        when {
            organisasjonsnummer.length == 9 -> InntektskildetypeDto.ORDINÆR
            else -> InntektskildetypeDto.ENKELTPERSONFORETAK
        }
}
