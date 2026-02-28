package no.nav.helse.db

import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import java.util.UUID

interface MeldingDao {
    fun finnGodkjenningsbehov(meldingId: UUID): Godkjenningsbehov

    fun finnSisteGodkjenningsbehov(spleisBehandlingId: UUID): Godkjenningsbehov?

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

    enum class Meldingtype {
        ADRESSEBESKYTTELSE_ENDRET,
        VEDTAKSPERIODE_FORKASTET,
        GODKJENNING,
        SAKSBEHANDLERLØSNING,
        OPPDATER_PERSONSNAPSHOT,
        UTBETALING_ENDRET,
        VEDTAKSPERIODE_REBEREGNET,
        BEHANDLING_OPPRETTET,
        GOSYS_OPPGAVE_ENDRET,
        ENDRET_EGEN_ANSATT_STATUS,
        NYE_VARSLER,
        SØKNAD_SENDT,
        VEDTAKSPERIODE_NY_UTBETALING,
        GODKJENT_TILBAKEDATERT_SYKMELDING,
        AVSLUTTET_UTEN_VEDTAK,
        AVSLUTTET_MED_VEDTAK,
        KLARGJØR_TILGANGSRELATERTE_DATA,
        STANS_AUTOMATISK_BEHANDLING,
    }

    fun lagre(
        id: UUID,
        json: String,
        meldingtype: Meldingtype,
        vedtaksperiodeId: UUID? = null,
    )
}
