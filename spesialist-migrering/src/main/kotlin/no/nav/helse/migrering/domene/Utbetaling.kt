package no.nav.helse.migrering.domene

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.migrering.domene.Varsel.Companion.konsumer

internal class Utbetaling(
    private val id: UUID,
    private val opprettet: LocalDateTime,
    private val oppdatert: LocalDateTime,
    private val status: String,
    private val vurdering: Vurdering?,
) {
    internal companion object {
        internal fun List<Utbetaling>.sortert(): List<Utbetaling> {
            return sortedBy { it.opprettet }
        }

        internal fun List<Utbetaling>.fjernGammelMoro(): List<Utbetaling> {
            val kopi = toMutableList()
            // Fjerner alle gamle utbetalinger som ikke ble utbetalt og som skulle ha vært forkastet men ikke har blitt det
            kopi.removeAll {
                it.status == "IKKE_UTBETALT" && indexOf(it) != lastIndex
            }
            return kopi
        }
    }

    internal fun lagGenerasjon(
        vedtaksperiodeId: UUID,
        sistOpprettet: LocalDateTime?,
        varsler: MutableList<Varsel>,
    ): Generasjon {
        val generasjonVarsler = varsler.konsumer(oppdatert)
        val låst = status in listOf("UTBETALT", "GODKJENT_UTEN_UTBETALING")
        return Generasjon(
            UUID.randomUUID(),
            vedtaksperiodeId,
            id,
            sistOpprettet ?: opprettet,
            if (låst) oppdatert else null,
            vurdering,
            generasjonVarsler,
        )
    }

    internal class Vurdering(
        internal val ident: String,
        internal val tidspunkt: LocalDateTime,
        internal val automatiskBehandling: Boolean,
        internal val godkjent: Boolean,
    ) {

    }
}