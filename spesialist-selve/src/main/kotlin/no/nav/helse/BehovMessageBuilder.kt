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
        is Behov.Arbeidsgiverinformasjon -> "Arbeidsgiverinformasjon"
        is Behov.EgenAnsatt -> "EgenAnsatt"
        is Behov.Enhet -> "HentEnhet"
        is Behov.Fullmakt -> "Fullmakt"
        is Behov.Infotrygdutbetalinger -> "HentInfotrygdutbetalinger"
        is Behov.InntekterForSykepengegrunnlag -> "InntekterForSykepengegrunnlag"
        is Behov.Personinfo -> "HentPersoninfoV2"
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
        is Behov.Arbeidsforhold -> somJsonMessage()
        is Behov.Arbeidsgiverinformasjon -> somJsonMessage()
        is Behov.Infotrygdutbetalinger -> somJsonMessage()
        is Behov.InntekterForSykepengegrunnlag -> somJsonMessage()
        is Behov.Personinfo -> somJsonMessage()
        is Behov.Risikovurdering -> somJsonMessage()
        is Behov.ÅpneOppgaver -> somJsonMessage()
    }
}

private fun Behov.Arbeidsforhold.somJsonMessage(): Map<String, Any> {
    return mapOf(
        "fødselsnummer" to fødselsnummer,
        "organisasjonsnummer" to organisasjonsnummer,
    )
}

private fun Behov.Arbeidsgiverinformasjon.somJsonMessage(): Map<String, Any> {
    return mapOf(
        "organisasjonsnummer" to this.organisasjonsnumre,
    )
}

private fun Behov.Infotrygdutbetalinger.somJsonMessage(): Map<String, Any> {
    return mapOf(
        "historikkFom" to this.fom,
        "historikkTom" to this.tom,
    )
}

private fun Behov.InntekterForSykepengegrunnlag.somJsonMessage(): Map<String, Any> {
    return mapOf(
        "beregningStart" to this.fom.toString(),
        "beregningSlutt" to this.tom.toString(),
    )
}

private fun Behov.Personinfo.somJsonMessage(): Map<String, Any> {
    val andreIdenter = this.andreIdenter
    return if (!andreIdenter.isNullOrEmpty()) mapOf("ident" to andreIdenter) else emptyMap()
}

private fun Behov.Risikovurdering.somJsonMessage(): Map<String, Any> {
    return mapOf(
        "vedtaksperiodeId" to vedtaksperiodeId,
        "organisasjonsnummer" to organisasjonsnummer,
        "førstegangsbehandling" to førstegangsbehandling,
        "kunRefusjon" to kunRefusjon,
    )
}

private fun Behov.ÅpneOppgaver.somJsonMessage(): Map<String, Any> {
    return mapOf("ikkeEldreEnn" to this.ikkeEldreEnn)
}
