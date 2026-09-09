package no.nav.helse.spesialist.application

import no.nav.helse.db.MeldingDao
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import java.util.UUID

class InMemoryMeldingDao : MeldingDao {
    val godkjenningsbehov = mutableListOf<Godkjenningsbehov>()
    internal val vedtaksperiodemeldinger = mutableListOf<Vedtaksperiodemelding>()
    private val automatiseringKorrigertSøknad = mutableListOf<AutomatiseringKorrigertSøknad>()
    private val behandlingOpprettetKorrigertSøknad = mutableMapOf<Pair<String, UUID>, MeldingDao.BehandlingOpprettetKorrigertSøknad>()

    internal data class Vedtaksperiodemelding(
        val id: UUID,
        val meldingtype: MeldingDao.Meldingtype,
        val vedtaksperiodeId: UUID,
    )

    private data class AutomatiseringKorrigertSøknad(
        val vedtaksperiodeId: UUID,
        val hendelseRef: UUID,
    )

    // Test-hjelpemetode: registrerer at det finnes en behandling opprettet pga. korrigert søknad for gitt vedtaksperiode.
    fun registrerBehandlingOpprettetKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
        meldingId: UUID,
    ) {
        behandlingOpprettetKorrigertSøknad[fødselsnummer to vedtaksperiodeId] =
            MeldingDao.BehandlingOpprettetKorrigertSøknad(meldingId = meldingId, vedtaksperiodeId = vedtaksperiodeId)
    }

    override fun finnGodkjenningsbehov(meldingId: UUID): Godkjenningsbehov = godkjenningsbehov.first { it.id == meldingId }

    override fun finnSisteGodkjenningsbehovOrNull(spleisBehandlingId: UUID): Godkjenningsbehov? = godkjenningsbehov.filter { it.spleisBehandlingId == spleisBehandlingId }.maxByOrNull { it.opprettet }

    override fun finnOrNull(id: UUID): Personmelding? {
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
    ): MeldingDao.BehandlingOpprettetKorrigertSøknad? = behandlingOpprettetKorrigertSøknad[fødselsnummer to vedtaksperiodeId]

    override fun erKorrigertSøknadTidligereAutomatiskBehandlet(meldingId: UUID): Boolean = automatiseringKorrigertSøknad.any { it.hendelseRef == meldingId }

    override fun antallGangerVedtaksperiodeErAutomatisertMedKorrigertSøknad(vedtaksperiodeId: UUID): Int = automatiseringKorrigertSøknad.count { it.vedtaksperiodeId == vedtaksperiodeId }

    override fun opprettAutomatiseringMedKorrigertSøknad(
        vedtaksperiodeId: UUID,
        meldingId: UUID,
    ) {
        automatiseringKorrigertSøknad.add(AutomatiseringKorrigertSøknad(vedtaksperiodeId, meldingId))
    }
}
