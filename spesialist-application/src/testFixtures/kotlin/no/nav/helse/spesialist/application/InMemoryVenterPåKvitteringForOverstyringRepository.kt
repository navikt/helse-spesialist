package no.nav.helse.spesialist.application

import no.nav.helse.db.overstyring.venting.MeldingId
import no.nav.helse.db.overstyring.venting.VenterPåKvitteringForOverstyring
import no.nav.helse.db.overstyring.venting.VenterPåKvitteringForOverstyringRepository

class InMemoryVenterPåKvitteringForOverstyringRepository :
    AbstractInMemoryRepository<MeldingId, VenterPåKvitteringForOverstyring>(),
    VenterPåKvitteringForOverstyringRepository {

    override fun deepCopy(original: VenterPåKvitteringForOverstyring): VenterPåKvitteringForOverstyring =
        VenterPåKvitteringForOverstyring.fraLagring(
            meldingId = original.id,
            identitetsnummer = original.identitetsnummer,
        )

}

