package no.nav.helse.spesialist.application

import no.nav.helse.db.MeldingDao
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import java.util.UUID

class InMemoryMeldingDao : MeldingDao {
    val godkjenningsbehov = mutableListOf<Godkjenningsbehov>()
    internal val vedtaksperiodemeldinger = mutableListOf<Vedtaksperiodemelding>()

    internal data class Vedtaksperiodemelding(
        val id: UUID,
        val meldingtype: MeldingDao.Meldingtype,
        val vedtaksperiodeId: UUID,
    )

    override fun finnGodkjenningsbehov(meldingId: UUID): Godkjenningsbehov = godkjenningsbehov.first { it.id == meldingId }

    override fun finnSisteGodkjenningsbehov(spleisBehandlingId: UUID): Godkjenningsbehov? = godkjenningsbehov.filter { it.spleisBehandlingId == spleisBehandlingId }.maxByOrNull { it.opprettet }

    override fun finn(id: UUID): Personmelding? {
        TODO("Not yet implemented")
    }

    override fun lagre(melding: Personmelding) {
        if (melding is Godkjenningsbehov) {
            godkjenningsbehov.add(melding)
        }
    }

    override fun lagre(
        id: UUID,
        json: String,
        meldingtype: MeldingDao.Meldingtype,
        vedtaksperiodeId: UUID?,
    ) {
        if (vedtaksperiodeId == null) return
        vedtaksperiodemeldinger.add(Vedtaksperiodemelding(id, meldingtype, vedtaksperiodeId))
    }

    override fun sisteBehandlingOpprettetOmKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
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
        meldingId: UUID,
    ) {
        TODO("Not yet implemented")
    }
}
