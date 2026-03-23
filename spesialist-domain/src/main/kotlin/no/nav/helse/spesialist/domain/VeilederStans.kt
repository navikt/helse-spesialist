package no.nav.helse.spesialist.domain

import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.time.Instant
import java.util.UUID

@JvmInline
value class VeilederStansId(
    val value: UUID,
) : ValueObject

class VeilederStans private constructor(
    id: VeilederStansId,
    val identitetsnummer: Identitetsnummer,
    val årsaker: Set<StansÅrsak>,
    val opprettet: Instant,
    val originalMeldingId: UUID,
    stansOpphevet: StansOpphevet?,
) : AggregateRoot<VeilederStansId>(id) {
    var stansOpphevet: StansOpphevet? = stansOpphevet
        private set
    val erStansett: Boolean
        get() = stansOpphevet == null

    fun opphevStans(
        opphevetAvSaksbehandlerIdent: NAVIdent,
        begrunnelse: String,
    ) {
        check(erStansett) { "Stansen er allerede opphevet" }
        stansOpphevet =
            StansOpphevet(
                opphevetAvSaksbehandlerIdent = opphevetAvSaksbehandlerIdent,
                begrunnelse = begrunnelse,
                opphevetTidspunkt = Instant.now(),
            )
    }

    data class StansOpphevet(
        val opphevetAvSaksbehandlerIdent: NAVIdent,
        val begrunnelse: String,
        val opphevetTidspunkt: Instant,
    )

    enum class StansÅrsak {
        MEDISINSK_VILKAR,
        AKTIVITETSKRAV,
        MANGLENDE_MEDVIRKING,

        // BESTRIDELSE_SYKMELDING er ikke lenger i bruk hos iSyfo, men spesialist har historiske meldinger med denne årsaken
        BESTRIDELSE_SYKMELDING,
    }

    companion object {
        fun ny(
            identitetsnummer: Identitetsnummer,
            årsaker: Set<StansÅrsak>,
            opprettet: Instant,
            originalMeldingId: UUID,
        ) = VeilederStans(
            id = VeilederStansId(UUID.randomUUID()),
            identitetsnummer = identitetsnummer,
            årsaker = årsaker,
            opprettet = opprettet,
            originalMeldingId = originalMeldingId,
            stansOpphevet = null,
        )

        fun fraLagring(
            id: VeilederStansId,
            identitetsnummer: Identitetsnummer,
            årsaker: Set<StansÅrsak>,
            opprettet: Instant,
            originalMeldingId: UUID,
            stansOpphevet: StansOpphevet?,
        ) = VeilederStans(
            id = id,
            identitetsnummer = identitetsnummer,
            årsaker = årsaker,
            opprettet = opprettet,
            originalMeldingId = originalMeldingId,
            stansOpphevet = stansOpphevet,
        )
    }
}
