package no.nav.helse.modell.kommando

import no.nav.helse.db.InntektskilderRepository
import no.nav.helse.modell.Inntektskilde
import no.nav.helse.modell.Inntektskildetype
import no.nav.helse.modell.KomplettInntektskilde
import no.nav.helse.modell.arbeidsgiver.Arbeidsgiverinformasjonløsning
import no.nav.helse.modell.person.HentPersoninfoløsninger

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

        inntektskilderSomSkalLagres.lagreOppdaterteInntektskilder()

        if (inntektskilderSomFortsattMåOppdateres.isEmpty()) return true
        sendBehov(context, inntektskilderSomFortsattMåOppdateres)
        return false
    }

    private fun sendBehov(
        context: CommandContext,
        inntektskilder: List<Inntektskilde>,
    ) {
        inntektskilder
            .lagBehov()
            .forEach { (behovKey, payload) ->
                context.behov(behovKey, payload)
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

    private fun List<Inntektskilde>.lagBehov(): Map<String, Map<String, List<String>>> {
        return this
            .groupBy(keySelector = { it.type }, valueTransform = { it.organisasjonsnummer })
            .map { (inntektskildetype, inntektskilder) ->
                when (inntektskildetype) {
                    Inntektskildetype.ORDINÆR -> "Arbeidsgiverinformasjon" to mapOf("organisasjonsnummer" to inntektskilder)
                    Inntektskildetype.ENKELTPERSONFORETAK -> "HentPersoninfoV2" to mapOf("ident" to inntektskilder)
                }
            }.toMap()
    }
}
