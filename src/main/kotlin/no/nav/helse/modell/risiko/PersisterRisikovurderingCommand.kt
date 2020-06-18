package no.nav.helse.modell.risiko

import kotliquery.Session
import no.nav.helse.mediator.kafka.meldinger.RisikovurderingMessage
import no.nav.helse.modell.command.RootCommand
import java.time.Duration
import java.util.*

class PersisterRisikovurderingCommand(
    eventId: UUID,
    val risikovurderingMessage: RisikovurderingMessage,
    override val vedtaksperiodeId: UUID
) : RootCommand(behovId = eventId, timeout = Duration.ZERO) {
    override val orgnummer: String? = null
    override val fødselsnummer: String get() = throw RuntimeException("Not implemented")

    override fun toJson(): String = "{}"

    override fun execute(session: Session): Resultat {
        val warnings = mutableListOf<String>()
        val risikovurdering = session.hentRisikovurderingForVedtaksperiode(vedtaksperiodeId)
            ?: return Resultat.HarBehov()
        val paragrafPrefix = "8-4: "
        val (arbeidsuførhetvurdering, faresignaler) = risikovurdering?.faresignaler
            .partition { it.startsWith(paragrafPrefix) }
            .let { it.first.map { begrunnelse -> begrunnelse.removePrefix(paragrafPrefix) } to it.second }
        if (arbeidsuførhetvurdering.isNotEmpty()) {
            warnings.add("Arbeidsuførhet må vurderes")
        }
        if (faresignaler.isNotEmpty() && risikovurdering?.samletScore ?: 0 > 0) {
            warnings.add("Faresignaler oppdaget")
        }

        session.persisterRisikovurdering(
            RisikovurderingDto(
                vedtaksperiodeId = risikovurderingMessage.vedtaksperiodeId,
                opprettet = risikovurderingMessage.opprettet,
                samletScore = risikovurderingMessage.samletScore,
                faresignaler = faresignaler,
                arbeidsuførhetvurdering = arbeidsuførhetvurdering,
                ufullstendig = risikovurderingMessage.ufullstendig
            )
        )

        return Resultat.Ok.System
    }
}
