package no.nav.helse.modell.kommando

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.HentPersoninfoløsninger
import no.nav.helse.spesialist.application.ArbeidsgiverRepository
import no.nav.helse.spesialist.application.logg.sikkerlogg
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
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
            identifikatorer = identifikatorer.map { ArbeidsgiverIdentifikator.fraString(it) }.toSet(),
        )

    private fun arbeidsgivereSomMåOppdateres(
        fødselsnummer: String,
        identifikatorer: Set<ArbeidsgiverIdentifikator>,
    ): List<Arbeidsgiver> {
        val alleIdentifikatorer =
            identifikatorer
                .filterNot(::erSelvstendig)
                .plus(organisasjonsnumreFraSammenligningsgrunnlag(fødselsnummer = fødselsnummer))
                .distinct()

        val eksisterendeArbeidsgivere =
            arbeidsgiverRepository.finnAlle(alleIdentifikatorer.toSet())

        val manglendeArbeidsgivere =
            alleIdentifikatorer.minus(eksisterendeArbeidsgivere.map(Arbeidsgiver::id).toSet())
        val nyeArbeidsgivere = manglendeArbeidsgivere.map { Arbeidsgiver.Factory.ny(identifikator = it) }
        return (eksisterendeArbeidsgivere + nyeArbeidsgivere).filter(::måOppdateres)
    }

    private fun erSelvstendig(it: ArbeidsgiverIdentifikator) =
        when (it) {
            is ArbeidsgiverIdentifikator.Fødselsnummer -> it.fødselsnummer
            is ArbeidsgiverIdentifikator.Organisasjonsnummer -> it.organisasjonsnummer
        } == SELVSTENDIG

    private fun organisasjonsnumreFraSammenligningsgrunnlag(fødselsnummer: String): List<ArbeidsgiverIdentifikator> =
        avviksvurderingRepository
            .finnAvviksvurderinger(fødselsnummer)
            .flatMap { it.sammenligningsgrunnlag.innrapporterteInntekter }
            .map {
                if (it.arbeidsgiverreferanse.length == 9) {
                    ArbeidsgiverIdentifikator.Organisasjonsnummer(it.arbeidsgiverreferanse)
                } else {
                    ArbeidsgiverIdentifikator.Fødselsnummer(it.arbeidsgiverreferanse)
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
                when (val identifikator = arbeidsgiver.id()) {
                    is ArbeidsgiverIdentifikator.Fødselsnummer -> {
                        personinfoløsninger?.relevantLøsning(identifikator.fødselsnummer)?.navn()
                    }

                    is ArbeidsgiverIdentifikator.Organisasjonsnummer -> {
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
                .map(Arbeidsgiver::id)
                .filterIsInstance<ArbeidsgiverIdentifikator.Fødselsnummer>()
                .map(ArbeidsgiverIdentifikator.Fødselsnummer::fødselsnummer)
                .takeUnless { it.isEmpty() }
                ?.let(Behov.Arbeidsgiverinformasjon::Enkeltpersonforetak)

        if (enkeltpersonforetakBehov != null) {
            context.behov(enkeltpersonforetakBehov)
        }

        val ordinærArbeidsgiverBehov =
            arbeidsgivere
                .map(Arbeidsgiver::id)
                .filterIsInstance<ArbeidsgiverIdentifikator.Organisasjonsnummer>()
                .map(ArbeidsgiverIdentifikator.Organisasjonsnummer::organisasjonsnummer)
                .takeUnless { it.isEmpty() }
                ?.let { Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver(it) }

        if (ordinærArbeidsgiverBehov != null) {
            context.behov(ordinærArbeidsgiverBehov)
        }
    }

    private fun måOppdateres(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.navnIkkeOppdatertSiden(LocalDate.now().minusDays(14))

    private fun Collection<Arbeidsgiver>.prettyPrint() = joinToString { "${it.id()} (${it.navn?.navn})" }

    companion object {
        private const val SELVSTENDIG = "SELVSTENDIG"
    }
}
