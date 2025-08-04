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
    private val alleIdentifikatorer =
        identifikatorer
            .filterNot { it == SELVSTENDIG }
            .map(ArbeidsgiverIdentifikator::fraString)
            .plus(organisasjonsnumreFraSammenligningsgrunnlag(fødselsnummer = fødselsnummer))
            .distinct()

    fun finnNyeOgUtdaterteArbeidsgivere(): Pair<Set<ArbeidsgiverIdentifikator>, List<Arbeidsgiver>> {
        val eksisterendeArbeidsgivere = arbeidsgiverRepository.finnAlle(alleIdentifikatorer.toSet())

        return Pair(
            alleIdentifikatorer.minus(eksisterendeArbeidsgivere.map(Arbeidsgiver::id).toSet()).toSet(),
            eksisterendeArbeidsgivere.filter(::måOppdateres),
        )
    }

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
        val (nyeArbeidsgiverIdentifikatorer, utdaterteArbeidsgivere) = finnNyeOgUtdaterteArbeidsgivere()
        if (nyeArbeidsgiverIdentifikatorer.isEmpty() && utdaterteArbeidsgivere.isEmpty()) return true
        if (nyeArbeidsgiverIdentifikatorer.isNotEmpty()) {
            sikkerlogg.info("Trenger navn på nye arbeidsgivere: ${nyeArbeidsgiverIdentifikatorer.joinToString()}")
        }
        if (utdaterteArbeidsgivere.isNotEmpty()) {
            sikkerlogg.info("Trenger oppdatert navn på kjente arbeidsgivere: ${utdaterteArbeidsgivere.joinToString { it.toLogString() }}")
        }
        sendBehov(context, nyeArbeidsgiverIdentifikatorer + utdaterteArbeidsgivere.map(Arbeidsgiver::id))
        return false
    }

    private fun Arbeidsgiver.toLogString(): String = "${id()}, $navn"

    override fun resume(context: CommandContext): Boolean {
        val (nyeArbeidsgiverIdentifikatorer, utdaterteArbeidsgivere) = finnNyeOgUtdaterteArbeidsgivere()

        nyeArbeidsgiverIdentifikatorer.forEach { identifikator ->
            val navnFraLøsning = finnNavnILøsninger(identifikator, context)
            if (navnFraLøsning != null) {
                val arbeidsgiver =
                    Arbeidsgiver.Factory.ny(
                        id = identifikator,
                        navnString = navnFraLøsning,
                    )
                sikkerlogg.info("Lagrer ny arbeidsgiver: ${arbeidsgiver.toLogString()}")
                arbeidsgiverRepository.lagre(arbeidsgiver)
            }
        }

        utdaterteArbeidsgivere.forEach { arbeidsgiver ->
            val identifikator = arbeidsgiver.id()
            val navnFraLøsning = finnNavnILøsninger(identifikator, context)
            if (navnFraLøsning != null) {
                arbeidsgiver.oppdaterMedNavn(navnFraLøsning)
                sikkerlogg.info("Lagrer oppdatert arbeidsgiver: ${arbeidsgiver.toLogString()}")
                arbeidsgiverRepository.lagre(arbeidsgiver)
            }
        }

        return execute(context)
    }

    private fun finnNavnILøsninger(
        identifikator: ArbeidsgiverIdentifikator,
        context: CommandContext,
    ): String? =
        when (identifikator) {
            is ArbeidsgiverIdentifikator.Fødselsnummer -> {
                context
                    .get<HentPersoninfoløsninger>()
                    ?.relevantLøsning(identifikator.fødselsnummer)
                    ?.navn()
            }

            is ArbeidsgiverIdentifikator.Organisasjonsnummer -> {
                context
                    .get<Arbeidsgiverinformasjonløsning>()
                    ?.relevantLøsning(identifikator.organisasjonsnummer)
                    ?.navn
            }
        }

    private fun sendBehov(
        context: CommandContext,
        arbeidsgiverIdentifikatorer: Set<ArbeidsgiverIdentifikator>,
    ) {
        val enkeltpersonforetakBehov =
            arbeidsgiverIdentifikatorer
                .filterIsInstance<ArbeidsgiverIdentifikator.Fødselsnummer>()
                .map(ArbeidsgiverIdentifikator.Fødselsnummer::fødselsnummer)
                .takeUnless { it.isEmpty() }
                ?.let(Behov.Arbeidsgiverinformasjon::Enkeltpersonforetak)

        if (enkeltpersonforetakBehov != null) {
            context.behov(enkeltpersonforetakBehov)
        }

        val ordinærArbeidsgiverBehov =
            arbeidsgiverIdentifikatorer
                .filterIsInstance<ArbeidsgiverIdentifikator.Organisasjonsnummer>()
                .map(ArbeidsgiverIdentifikator.Organisasjonsnummer::organisasjonsnummer)
                .takeUnless { it.isEmpty() }
                ?.let { Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver(it) }

        if (ordinærArbeidsgiverBehov != null) {
            context.behov(ordinærArbeidsgiverBehov)
        }
    }

    private fun måOppdateres(arbeidsgiver: Arbeidsgiver) = arbeidsgiver.navn.ikkeOppdatertSiden(LocalDate.now().minusDays(14))

    companion object {
        private const val SELVSTENDIG = "SELVSTENDIG"
    }
}
