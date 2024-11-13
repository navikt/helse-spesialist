package no.nav.helse.modell.kommando

import no.nav.helse.db.InntektskilderRepository
import no.nav.helse.modell.Inntektskilde
import no.nav.helse.modell.Inntektskildetype
import no.nav.helse.modell.KomplettInntektskilde
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.behov.Behov
import no.nav.helse.modell.person.HentPersoninfoløsninger
import org.slf4j.LoggerFactory

internal class OpprettEllerOppdaterInntektskilder(
    inntektskilder: List<Inntektskilde>,
    private val inntektskilderRepository: InntektskilderRepository,
) : Command {
    private val inntektskilderSomMåOppdateres = inntektskilder.somMåOppdateres()

    override fun execute(context: CommandContext): Boolean {
        if (inntektskilderSomMåOppdateres.isEmpty()) return true
        sendBehov(context, inntektskilderSomMåOppdateres)
        return false
    }

    override fun resume(context: CommandContext): Boolean {
        val (inntektskilderSomFortsattMåOppdateres, inntektskilderSomSkalLagres) =
            inntektskilderSomMåOppdateres
                .supplerMedLøsninger(context)
                .partition { it.måOppdateres() }

        sikkerlogg.info("Lagrer oppdaterte inntektskilder: ${inntektskilderSomSkalLagres.prettyPrint()}")
        inntektskilderSomSkalLagres.lagreOppdaterteInntektskilder()

        if (inntektskilderSomFortsattMåOppdateres.isEmpty()) return true
        sikkerlogg.info("Trenger fortsatt oppdatert info for inntektskilder: ${inntektskilderSomFortsattMåOppdateres.prettyPrint()}")
        sendBehov(context, inntektskilderSomFortsattMåOppdateres)
        return false
    }

    private fun sendBehov(
        context: CommandContext,
        inntektskilder: List<Inntektskilde>,
    ) {
        inntektskilder
            .lagBehov()
            .forEach {
                context.behov(it)
            }
    }

    private fun List<Inntektskilde>.somMåOppdateres() = filter { it.måOppdateres() }

    private fun List<Inntektskilde>.lagreOppdaterteInntektskilder() {
        val inntektskilderSomSkalLagres =
            this
                .filterIsInstance<KomplettInntektskilde>()
                .map { it.toDto() }
        inntektskilderRepository.lagreInntektskilder(inntektskilderSomSkalLagres)
    }

    private fun List<Inntektskilde>.supplerMedLøsninger(context: CommandContext): List<Inntektskilde> {
        val arbeidsgiverinformasjonløsning = context.get<Arbeidsgiverinformasjonløsning>()
        val personinfoløsninger = context.get<HentPersoninfoløsninger>()
        return this.map {
            it.mottaLøsninger(arbeidsgiverinformasjonløsning, personinfoløsninger)
        }
    }

    private fun List<Inntektskilde>.lagBehov(): List<Behov> {
        return this
            .groupBy(keySelector = { it.type }, valueTransform = { it.identifikator })
            .map { (inntektskildetype, inntektskilder) ->
                when (inntektskildetype) {
                    Inntektskildetype.ORDINÆR -> Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver(inntektskilder)
                    Inntektskildetype.ENKELTPERSONFORETAK -> Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak(inntektskilder)
                }
            }
    }

    companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    }
}

private fun Collection<Inntektskilde>.prettyPrint() = joinToString { "${it.identifikator} (${it.type})" }
