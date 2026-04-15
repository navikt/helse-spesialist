package no.nav.helse.db.overstyring.venting

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.ddd.AggregateRoot
import no.nav.helse.spesialist.domain.ddd.ValueObject
import java.util.UUID

class VenterPåKvitteringForOverstyring private constructor(
    meldingId: MeldingId,
    val identitetsnummer: Identitetsnummer,
) : AggregateRoot<MeldingId>(meldingId) {
    companion object {
        fun ny(
            meldingId: MeldingId,
            identitetsnummer: Identitetsnummer,
        ) = VenterPåKvitteringForOverstyring(
            meldingId = meldingId,
            identitetsnummer = identitetsnummer,
        )

        fun fraLagring(
            meldingId: MeldingId,
            identitetsnummer: Identitetsnummer,
        ) = VenterPåKvitteringForOverstyring(
            meldingId = meldingId,
            identitetsnummer = identitetsnummer,
        )
    }
}

@JvmInline
value class MeldingId(
    val value: UUID,
) : ValueObject
