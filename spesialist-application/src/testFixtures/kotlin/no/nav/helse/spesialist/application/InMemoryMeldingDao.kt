package no.nav.helse.spesialist.application

import no.nav.helse.db.MeldingDao
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import java.util.UUID

class InMemoryMeldingDao : MeldingDao {
    val godkjenningsbehov = mutableListOf<Godkjenningsbehov>()

    override fun finnGodkjenningsbehov(meldingId: UUID): Godkjenningsbehov {
        TODO("Not yet implemented")
    }

    override fun finnSisteGodkjenningsbehov(spleisBehandlingId: UUID): Godkjenningsbehov? {
        return godkjenningsbehov.filter { it.spleisBehandlingId == spleisBehandlingId }.maxByOrNull { it.opprettet }
    }

    override fun finn(id: UUID): Personmelding? {
        TODO("Not yet implemented")
    }

    override fun lagre(melding: Personmelding) {}

    override fun lagre(
        id: UUID,
        json: String,
        meldingtype: MeldingDao.Meldingtype,
        vedtaksperiodeId: UUID?
    ) {
    }

    override fun sisteBehandlingOpprettetOmKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID
    ): MeldingDao.BehandlingOpprettetKorrigertSøknad? {
        TODO("Not yet implemented")
    }

    override fun erKorrigertSøknadAutomatiskBehandlet(meldingId: UUID): Boolean {
        TODO("Not yet implemented")
    }

    override fun finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId: UUID): Int {
        TODO("Not yet implemented")
    }

    override fun opprettAutomatiseringKorrigertSøknad(
        vedtaksperiodeId: UUID,
        meldingId: UUID
    ) {
        TODO("Not yet implemented")
    }
}
