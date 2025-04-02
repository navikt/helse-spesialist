package no.nav.helse.db

import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import java.util.UUID

interface MeldingDao {
    fun finnGodkjenningsbehov(meldingId: UUID): Godkjenningsbehov

    fun finn(id: UUID): Personmelding?

    fun lagre(melding: Personmelding)

    fun sisteBehandlingOpprettetOmKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
    ): BehandlingOpprettetKorrigertSøknad?

    fun erKorrigertSøknadAutomatiskBehandlet(meldingId: UUID): Boolean

    fun finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId: UUID): Int

    fun opprettAutomatiseringKorrigertSøknad(
        vedtaksperiodeId: UUID,
        meldingId: UUID,
    )

    data class BehandlingOpprettetKorrigertSøknad(
        val meldingId: UUID,
        val vedtaksperiodeId: UUID,
    )
}
