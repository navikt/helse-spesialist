package no.nav.helse

import no.nav.helse.modell.behov.Behov
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

internal fun Collection<Behov>.somJsonMessage(
    contextId: UUID,
    fødselsnummer: String,
    hendelseId: UUID,
): JsonMessage {
    return JsonMessage.newNeed(
        behov = this.map { behov -> behov.behovName() },
        map =
            this.associate {
                it.behovName() to it.somJsonMessage()
            } +
                mapOf(
                    "fødselsnummer" to fødselsnummer,
                    "contextId" to contextId,
                    "hendelseId" to hendelseId,
                ),
    )
}

internal fun Behov.behovName() =
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
    }

private fun Behov.somJsonMessage(): Map<String, Any> {
    return when (this) {
        is Behov.EgenAnsatt -> emptyMap()
        is Behov.Enhet -> emptyMap()
        is Behov.Fullmakt -> emptyMap()
        is Behov.Vergemål -> emptyMap()
        is Behov.Arbeidsforhold -> detaljer()
        is Behov.Arbeidsgiverinformasjon.OrdinærArbeidsgiver -> detaljer()
        is Behov.Arbeidsgiverinformasjon.Enkeltpersonforetak -> detaljer()
        is Behov.Infotrygdutbetalinger -> detaljer()
        is Behov.InntekterForSykepengegrunnlag -> detaljer()
        is Behov.Personinfo -> emptyMap()
        is Behov.Risikovurdering -> detaljer()
        is Behov.ÅpneOppgaver -> detaljer()
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

private fun Behov.Risikovurdering.detaljer(): Map<String, Any> {
    return mapOf(
        "vedtaksperiodeId" to vedtaksperiodeId,
        "organisasjonsnummer" to organisasjonsnummer,
        "førstegangsbehandling" to førstegangsbehandling,
        "kunRefusjon" to kunRefusjon,
    )
}

private fun Behov.ÅpneOppgaver.detaljer(): Map<String, Any> {
    return mapOf("ikkeEldreEnn" to this.ikkeEldreEnn)
}
