package no.nav.helse.db

import kotliquery.Session
import no.nav.helse.HelseDao.Companion.asSQL
import no.nav.helse.HelseDao.Companion.asSQLWithQuestionMarks
import no.nav.helse.HelseDao.Companion.list
import no.nav.helse.modell.InntektskildeDto
import no.nav.helse.modell.Inntektskildetype
import no.nav.helse.modell.InntektskildetypeDto
import no.nav.helse.modell.KomplettInntektskildeDto
import no.nav.helse.modell.NyInntektskilde
import no.nav.helse.modell.NyInntektskildeDto

internal class InntektskilderDao(
    private val session: Session,
) : InntektskilderRepository {
    private val arbeidsgiverDao = ArbeidsgiverDao(session)
    private val avviksvurderingDao = PgAvviksvurderingDao(session)

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

    override fun finnInntektskilderSomManglerNavnForAktiveOppgaver() =
        asSQL(
            """
            with utvalg as (
                select p.id from person p
                join vedtak v on p.id = v.person_ref
                join oppgave o on v.id = o.vedtak_ref and o.status = 'AvventerSaksbehandler'
            )
            select organisasjonsnummer
            from utvalg u
            join vedtak v on u.id = v.person_ref
            join arbeidsgiver a on v.arbeidsgiver_ref = a.id
            left join arbeidsgiver_navn an on a.navn_ref = an.id
            where an.id is null;
            """.trimIndent(),
        ).list(session) {
            it.string("organisasjonsnummer")
        }.map {
            val type =
                when {
                    it.length == 9 -> Inntektskildetype.ORDINÆR
                    else -> Inntektskildetype.ENKELTPERSONFORETAK
                }
            NyInntektskilde(it, type)
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
                identifikator = organisasjonsnummerEllerFødselsnummer(identifikator),
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
        avviksvurderingDao
            .finnAvviksvurderinger(fødselsnummer)
            .flatMap { it.sammenligningsgrunnlag.innrapporterteInntekter }
            .map { it.arbeidsgiverreferanse }

    private fun inntektskildetype(organisasjonsnummer: String): InntektskildetypeDto =
        when {
            organisasjonsnummer.length == 9 -> InntektskildetypeDto.ORDINÆR
            else -> InntektskildetypeDto.ENKELTPERSONFORETAK
        }

    private fun organisasjonsnummerEllerFødselsnummer(organisasjonsnummer: String): String =
        when {
            organisasjonsnummer.length == 10 -> organisasjonsnummer.toFødselsnummer()
            else -> organisasjonsnummer
        }

    private fun String.toFødselsnummer() = "0$this"
}
