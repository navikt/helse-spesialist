package no.nav.helse.db.overstyring.venting

interface VenterPåKvitteringForOverstyringRepository {
    fun finnOrNull(meldingId: MeldingId): VenterPåKvitteringForOverstyring?

    fun lagre(venterPåKvitteringForOverstyring: VenterPåKvitteringForOverstyring)

    fun slett(meldingId: MeldingId)
}
