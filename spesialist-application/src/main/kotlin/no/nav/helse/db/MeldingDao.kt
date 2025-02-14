package no.nav.helse.db

import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import java.time.LocalDate
import java.util.UUID

interface MeldingDao {
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

    data class OverstyringIgangsattKorrigertSøknad(
        val periodeForEndringFom: LocalDate,
        val meldingId: String,
        val berørtePerioder: List<BerørtPeriode>,
    )

    data class BerørtPeriode(
        val vedtaksperiodeId: UUID,
        val periodeFom: LocalDate,
        val orgnummer: String,
    )
}
