package no.nav.helse.modell.kommando

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentPersoninfoløsninger
import no.nav.helse.spesialist.application.ArbeidsgiverRepository
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Arbeidsgiver
import java.time.LocalDate

internal class OpprettEllerOppdaterInntektskilder(
    fødselsnummer: String,
    identifikatorer: Set<String>,
    private val arbeidsgiverRepository: ArbeidsgiverRepository,
    private val avviksvurderingRepository: AvviksvurderingRepository,
) : Command {
    private val arbeidsgivereSomMåOppdateres =
        arbeidsgivereSomMåOppdateres(
            fødselsnummer = fødselsnummer,
            identifikatorer = identifikatorer.map { Arbeidsgiver.Identifikator.fraString(it) }.toSet(),
        )

    private fun arbeidsgivereSomMåOppdateres(
        fødselsnummer: String,
        identifikatorer: Set<Arbeidsgiver.Identifikator>,
    ): List<Arbeidsgiver> {
        val alleIdentifikatorer =
            identifikatorer
                .filterNot(::erSelvstendig)
                .plus(organisasjonsnumreFraSammenligningsgrunnlag(fødselsnummer = fødselsnummer))
                .distinct()

        val eksisterendeArbeidsgivere =
            arbeidsgiverRepository.finnAlleForIdentifikatorer(alleIdentifikatorer.toSet())

        val manglendeArbeidsgivere =
            alleIdentifikatorer.minus(eksisterendeArbeidsgivere.map(Arbeidsgiver::identifikator).toSet())
        val nyeArbeidsgivere = manglendeArbeidsgivere.map { Arbeidsgiver.Factory.ny(identifikator = it) }
        return (eksisterendeArbeidsgivere + nyeArbeidsgivere).filter(::måOppdateres)
    }

    private fun erSelvstendig(it: Arbeidsgiver.Identifikator) =
        when (it) {
            is Arbeidsgiver.Identifikator.Fødselsnummer -> it.fødselsnummer
            is Arbeidsgiver.Identifikator.Organisasjonsnummer -> it.organisasjonsnummer
        } == SELVSTENDIG

    private fun organisasjonsnumreFraSammenligningsgrunnlag(fødselsnummer: String): List<Arbeidsgiver.Identifikator> =
        avviksvurderingRepository
            .finnAvviksvurderinger(fødselsnummer)
            .flatMap { it.sammenligningsgrunnlag.innrapporterteInntekter }
            .map {
                if (it.arbeidsgiverreferanse.length == 9) {
                    Arbeidsgiver.Identifikator.Organisasjonsnummer(it.arbeidsgiverreferanse)
                } else {
                    Arbeidsgiver.Identifikator.Fødselsnummer(it.arbeidsgiverreferanse)
                }
            }

    override fun execute(context: CommandContext): Boolean {
        if (arbeidsgivereSomMåOppdateres.isEmpty()) return true
        sendBehov(context, arbeidsgivereSomMåOppdateres)
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        arbeidsgivereSomMåOppdateres.oppdaterMedNavnFraLøsninger(context)

        val (arbeidsgivereSomFortsattMåOppdateres, arbeidsgivereSomSkalLagres) =
            arbeidsgivereSomMåOppdateres.partition(::måOppdateres)

        sikkerlogg.info("Lagrer oppdaterte arbeidsgivere: ${arbeidsgivereSomSkalLagres.prettyPrint()}")
        arbeidsgivereSomSkalLagres.forEach { arbeidsgiverRepository.lagre(it) }

        if (arbeidsgivereSomFortsattMåOppdateres.isEmpty()) return true
        sikkerlogg.info("Trenger fortsatt oppdatert info for arbeidsgivere: ${arbeidsgivereSomFortsattMåOppdateres.prettyPrint()}")
        sendBehov(context, arbeidsgivereSomFortsattMåOppdateres)
        return false
    }

    private fun List<Arbeidsgiver>.oppdaterMedNavnFraLøsninger(context: CommandContext) {
        val arbeidsgiverinformasjonløsning = context.get<Arbeidsgiverinformasjonløsning>()
        val personinfoløsninger = context.get<HentPersoninfoløsninger>()
        forEach { arbeidsgiver ->
            val navnFraLøsning =
                when (val identifikator = arbeidsgiver.identifikator) {
                    is Arbeidsgiver.Identifikator.Fødselsnummer -> {
                        personinfoløsninger?.relevantLøsning(identifikator.fødselsnummer)?.navn()
                    }

                    is Arbeidsgiver.Identifikator.Organisasjonsnummer -> {
                        arbeidsgiverinformasjonløsning?.relevantLøsning(identifikator.organisasjonsnummer)?.navn
                    }
                }
            if (navnFraLøsning != null) {
                arbeidsgiver.oppdaterMedNavn(navnFraLøsning)
            }
        }
    }

    private fun sendBehov(
        context: CommandContext,
        arbeidsgivere: List<Arbeidsgiver>,
    ) {
        val enkeltpersonforetakBehov =
            arbeidsgivere
                .map(Arbeidsgiver::identifikator)
                .filterIsInstance<Arbeidsgiver.Identifikator.Fødselsnummer>()
                .map(Arbeidsgiver.Identifikator.Fødselsnummer::fødselsnummer)
                .takeUnless { it.isEmpty() }
                ?.let(Behov.Arbeidsgiverinformasjon::Enkeltpersonforetak)

        if (enkeltpersonforetakBehov != null) {
            context.behov(enkeltpersonforetakBehov)
        }

        val ordinærArbeidsgiverBehov =
            arbeidsgivere
                .map(Arbeidsgiver::identifikator)
                .filterIsInstance<Arbeidsgiver.Identifikator.Organisasjonsnummer>()
                .map(Arbeidsgiver.Identifikator.Organisasjonsnummer::organisasjonsnummer)
                .takeUnless { it.isEmpty() }
                ?.let { Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver(it) }

        if (ordinærArbeidsgiverBehov != null) {
            context.behov(ordinærArbeidsgiverBehov)
        }
    }

    private fun måOppdateres(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.navnIkkeOppdatertSiden(LocalDate.now().minusDays(14))

    private fun Collection<Arbeidsgiver>.prettyPrint() = joinToString { "${it.identifikator} (${it.navn?.navn})" }

    companion object {
        private const val SELVSTENDIG = "SELVSTENDIG"
    }
}
