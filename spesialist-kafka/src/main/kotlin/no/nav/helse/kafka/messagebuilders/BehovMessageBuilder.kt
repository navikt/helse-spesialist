package no.nav.helse.kafka.messagebuilders

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.helse.modell.melding.Behov
import java.util.UUID

fun Collection<Behov>.somJsonMessage(
    contextId: UUID,
    fødselsnummer: String,
    hendelseId: UUID,
): JsonMessage {
    return JsonMessage.newNeed(
        behov = this.map { behov -> behov.behovName() },
        map =
            this.associate {
                it.behovName() to it.detaljer()
            } +
                mapOf(
                    "fødselsnummer" to fødselsnummer,
                    "contextId" to contextId,
                    "hendelseId" to hendelseId,
                ),
    )
}

fun Behov.behovName() =
    when (this) {
        is Behov.Arbeidsforhold -> "Arbeidsforhold"
        is Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver -> "Arbeidsgiverinformasjon"
        is Behov.Enhet -> "HentEnhet"
        is Behov.Fullmakt -> "Fullmakt"
        is Behov.Infotrygdutbetalinger -> "HentInfotrygdutbetalinger"
        is Behov.InntekterForSykepengegrunnlag -> "InntekterForSykepengegrunnlag"
        is Behov.EgenAnsatt -> "EgenAnsatt"
        is Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak,
        is Behov.Personinfo,
        -> "HentPersoninfoV2"
        is Behov.Risikovurdering -> "Risikovurdering"
        is Behov.Vergemål -> "Vergemål"
        is Behov.ÅpneOppgaver -> "ÅpneOppgaver"
        is Behov.Avviksvurdering -> error("Ikke implementert enda")
    }

private fun Behov.detaljer(): Map<String, Any?> {
    return when (this) {
        is Behov.EgenAnsatt -> emptyMap()
        is Behov.Enhet -> emptyMap()
        is Behov.Fullmakt -> emptyMap()
        is Behov.Vergemål -> emptyMap()
        is Behov.Arbeidsforhold -> this.detaljer()
        is Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver -> this.detaljer()
        is Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak -> this.detaljer()
        is Behov.Infotrygdutbetalinger -> this.detaljer()
        is Behov.InntekterForSykepengegrunnlag -> this.detaljer()
        is Behov.Personinfo -> emptyMap()
        is Behov.Risikovurdering -> this.detaljer()
        is Behov.ÅpneOppgaver -> this.detaljer()
        is Behov.Avviksvurdering -> error("Ikke implementert enda")
    }
}

private fun Behov.Arbeidsforhold.detaljer(): Map<String, Any> {
    return mapOf(
        "fødselsnummer" to fødselsnummer,
        "organisasjonsnummer" to organisasjonsnummer,
    )
}

private fun Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver.detaljer(): Map<String, Any> {
    return mapOf(
        "organisasjonsnummer" to this.organisasjonsnumre,
    )
}

private fun Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak.detaljer(): Map<String, Any> {
    return mapOf(
        "ident" to this.identer,
    )
}

private fun Behov.Infotrygdutbetalinger.detaljer(): Map<String, Any> {
    return mapOf(
        "historikkFom" to this.fom,
        "historikkTom" to this.tom,
    )
}

private fun Behov.InntekterForSykepengegrunnlag.detaljer(): Map<String, Any> {
    return mapOf(
        "beregningStart" to this.fom.toString(),
        "beregningSlutt" to this.tom.toString(),
    )
}

private fun Behov.Risikovurdering.detaljer(): Map<String, Any?> {
    return mapOf(
        "vedtaksperiodeId" to vedtaksperiodeId,
        "organisasjonsnummer" to organisasjonsnummer,
        "førstegangsbehandling" to førstegangsbehandling,
        "kunRefusjon" to kunRefusjon,
        "inntekt" to inntekt,
    )
}

private fun Behov.ÅpneOppgaver.detaljer(): Map<String, Any> {
    return mapOf("ikkeEldreEnn" to this.ikkeEldreEnn)
}
