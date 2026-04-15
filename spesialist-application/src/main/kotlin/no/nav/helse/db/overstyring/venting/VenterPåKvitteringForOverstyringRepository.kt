package no.nav.helse.db.overstyring.venting

interface VenterPåKvitteringForOverstyringRepository {
    fun finn(meldingId: MeldingId): VenterPåKvitteringForOverstyring?

    fun lagre(venterPåKvitteringForOverstyring: VenterPåKvitteringForOverstyring)
}
