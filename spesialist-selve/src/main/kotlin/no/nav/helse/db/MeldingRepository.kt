package no.nav.helse.db

import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.MeldingDao.OverstyringIgangsattKorrigertSøknad
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import java.util.UUID

internal interface MeldingRepository {
    fun finnGodkjenningsbehov(meldingId: UUID): Godkjenningsbehov

    fun finn(id: UUID): Personmelding?

    fun lagre(melding: Personmelding)

    fun sisteOverstyringIgangsattOmKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
    ): OverstyringIgangsattKorrigertSøknad?

    fun erKorrigertSøknadAutomatiskBehandlet(meldingId: UUID): Boolean

    fun finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId: UUID): Int

    fun opprettAutomatiseringKorrigertSøknad(
        vedtaksperiodeId: UUID,
        meldingId: UUID,
    )
}
