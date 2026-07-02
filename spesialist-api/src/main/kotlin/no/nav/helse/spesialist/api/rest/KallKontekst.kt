package no.nav.helse.spesialist.api.rest

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.ktor.server.routing.RoutingCall
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.auth.AccessToken
import no.nav.helse.spesialist.api.medMdcOgAttribute
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.PersonPseudoIdProvider
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.BehandlingUnikId
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import no.nav.helse.spesialist.domain.oppgave.Oppgave
import no.nav.helse.spesialist.domain.oppgave.OppgaveId
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class KallKontekst(
    val saksbehandler: Saksbehandler,
    val brukerroller: Set<Brukerrolle>,
    val tilganger: Set<Tilgang>,
    val transaksjon: SessionContext,
    val outbox: Outbox,
    val personPseudoIdProvider: PersonPseudoIdProvider,
    val populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
    val accessToken: AccessToken,
    private val ktorCall: RoutingCall,
) {
    fun <RESPONSE, ERROR : ApiErrorCode> medOppgave(
        oppgaveId: OppgaveId,
        oppgaveIkkeFunnet: () -> ERROR,
        manglerTilgangTilPerson: () -> ERROR,
        block: (Oppgave, Behandling, Vedtaksperiode, Person) -> RestResponse<RESPONSE, ERROR>,
    ): RestResponse<RESPONSE, ERROR> =
        medMdcOgAttribute(MdcKey.OPPGAVE_ID to oppgaveId.value.toString()) {
            val oppgave = transaksjon.oppgaveRepository.finn(oppgaveId)

            if (oppgave == null) {
                loggWarn("Oppgaven ble ikke funnet", "oppgaveId" to oppgaveId)
                return@medMdcOgAttribute RestResponse.Error(oppgaveIkkeFunnet())
            }

            medBehandling(
                spleisBehandlingId = oppgave.behandlingId,
                behandlingIkkeFunnet = { error("Behandlingen ble ikke funnet") },
                manglerTilgangTilPerson = manglerTilgangTilPerson,
            ) { behandling, vedtaksperiode, person -> block(oppgave, behandling, vedtaksperiode, person) }
        }

    fun <RESPONSE, ERROR : ApiErrorCode> medBehandling(
        spleisBehandlingId: SpleisBehandlingId,
        behandlingIkkeFunnet: () -> ERROR,
        manglerTilgangTilPerson: () -> ERROR,
        block: (Behandling, Vedtaksperiode, Person) -> RestResponse<RESPONSE, ERROR>,
    ): RestResponse<RESPONSE, ERROR> =
        medMdcOgAttribute(MdcKey.SPLEIS_BEHANDLING_ID to spleisBehandlingId.value.toString()) {
            val behandling = transaksjon.behandlingRepository.finn(spleisBehandlingId)

            if (behandling == null) {
                loggWarn("Behandlingen ble ikke funnet", "spleisBehandlingId" to spleisBehandlingId)
                return@medMdcOgAttribute RestResponse.Error(behandlingIkkeFunnet())
            }

            medMdcOgAttribute(MdcKey.BEHANDLING_UNIK_ID to behandling.id.value.toString()) {
                medVedtaksperiode(
                    vedtaksperiodeId = behandling.vedtaksperiodeId,
                    vedtaksperiodeIkkeFunnet = { error("Vedtaksperioden ble ikke funnet") },
                    manglerTilgangTilPerson = manglerTilgangTilPerson,
                ) { vedtaksperiode, person -> block(behandling, vedtaksperiode, person) }
            }
        }

    fun <RESPONSE, ERROR : ApiErrorCode> medBehandling(
        behandlingUnikId: BehandlingUnikId,
        behandlingIkkeFunnet: () -> ERROR,
        manglerTilgangTilPerson: () -> ERROR,
        block: (Behandling, Vedtaksperiode, Person) -> RestResponse<RESPONSE, ERROR>,
    ): RestResponse<RESPONSE, ERROR> =
        medMdcOgAttribute(MdcKey.BEHANDLING_UNIK_ID to behandlingUnikId.value.toString()) {
            val behandling = transaksjon.behandlingRepository.finn(behandlingUnikId)

            if (behandling == null) {
                loggWarn("Behandlingen ble ikke funnet", "behandlingUnikId" to behandlingUnikId)
                return@medMdcOgAttribute RestResponse.Error(behandlingIkkeFunnet())
            }

            val spleisBehandlingId = behandling.spleisBehandlingId
            if (spleisBehandlingId == null) {
                medVedtaksperiode(
                    vedtaksperiodeId = behandling.vedtaksperiodeId,
                    vedtaksperiodeIkkeFunnet = { error("Vedtaksperioden ble ikke funnet") },
                    manglerTilgangTilPerson = manglerTilgangTilPerson,
                ) { vedtaksperiode, person -> block(behandling, vedtaksperiode, person) }
            } else {
                medMdcOgAttribute(MdcKey.SPLEIS_BEHANDLING_ID to spleisBehandlingId.value.toString()) {
                    medVedtaksperiode(
                        vedtaksperiodeId = behandling.vedtaksperiodeId,
                        vedtaksperiodeIkkeFunnet = { error("Vedtaksperioden ble ikke funnet") },
                        manglerTilgangTilPerson = manglerTilgangTilPerson,
                    ) { vedtaksperiode, person -> block(behandling, vedtaksperiode, person) }
                }
            }
        }

    fun <RESPONSE, ERROR : ApiErrorCode> medVedtaksperiode(
        vedtaksperiodeId: VedtaksperiodeId,
        vedtaksperiodeIkkeFunnet: () -> ERROR,
        manglerTilgangTilPerson: () -> ERROR,
        block: (Vedtaksperiode, Person) -> RestResponse<RESPONSE, ERROR>,
    ): RestResponse<RESPONSE, ERROR> =
        medMdcOgAttribute(MdcKey.VEDTAKSPERIODE_ID to vedtaksperiodeId.value.toString()) {
            val vedtaksperiode = transaksjon.vedtaksperiodeRepository.finn(vedtaksperiodeId)

            if (vedtaksperiode == null) {
                loggWarn("Vedtaksperioden ble ikke funnet", "vedtaksperiodeId" to vedtaksperiodeId)
                return@medMdcOgAttribute RestResponse.Error(vedtaksperiodeIkkeFunnet())
            }

            medPerson(
                identitetsnummer = vedtaksperiode.identitetsnummer,
                personIkkeFunnet = { error("Personen ble ikke funnet") },
                manglerTilgangTilPerson = manglerTilgangTilPerson,
            ) { person -> block(vedtaksperiode, person) }
        }

    fun <RESPONSE, ERROR : ApiErrorCode> medPerson(
        personPseudoId: PersonPseudoId,
        personPseudoIdIkkeFunnet: () -> ERROR,
        manglerTilgangTilPerson: () -> ERROR,
        block: (Person) -> RestResponse<RESPONSE, ERROR>,
    ): RestResponse<RESPONSE, ERROR> =
        medMdcOgAttribute(MdcKey.PERSON_PSEUDO_ID to personPseudoId.value.toString()) {
            val identitetsnummer = personPseudoIdProvider.hentIdentitetsnummer(personPseudoId)

            if (identitetsnummer == null) {
                loggWarn("Identitetsnummeret ble ikke funnet", "personPseudoId" to personPseudoId)
                return@medMdcOgAttribute RestResponse.Error(personPseudoIdIkkeFunnet())
            }

            medPerson(
                identitetsnummer = identitetsnummer,
                personIkkeFunnet = { error("Personen ble ikke funnet") },
                manglerTilgangTilPerson = manglerTilgangTilPerson,
                block = block,
            )
        }

    fun <RESPONSE, ERROR : ApiErrorCode> medPerson(
        identitetsnummer: Identitetsnummer,
        personIkkeFunnet: () -> ERROR,
        manglerTilgangTilPerson: () -> ERROR,
        block: (Person) -> RestResponse<RESPONSE, ERROR>,
    ): RestResponse<RESPONSE, ERROR> =
        medMdcOgAttribute(MdcKey.IDENTITETSNUMMER to identitetsnummer.value) {
            val person = transaksjon.personRepository.finn(identitetsnummer)

            if (person == null) {
                loggWarn("Personen ble ikke funnet", "identitetsnummer" to identitetsnummer)
                return@medMdcOgAttribute RestResponse.Error(personIkkeFunnet())
            }

            when (val resultat = populasjonstilgangskontrollProvider.kontrollerKjerneTilgang(accessToken.value, person.id.value)) {
                TilgangskontrollResultat.IdentIkkeFunnet -> return@medMdcOgAttribute RestResponse.Error(personIkkeFunnet())
                is TilgangskontrollResultat.ManglerTilgang -> {
                    loggWarn("Saksbehandler har ikke tilgang til personen", "identitetsnummer" to identitetsnummer)
                    return@medMdcOgAttribute RestResponse.Error(manglerTilgangTilPerson())
                }
                is TilgangskontrollResultat.UventetFeil -> error("Uventet feil fra Tilgangsmaskinen: ${resultat.menneskeligLesbarForklaring}")
                TilgangskontrollResultat.Ok -> block(person)
            }
        }

    fun <T> medMdcOgAttribute(
        pair: Pair<MdcKey, String>,
        block: () -> T,
    ): T = ktorCall.medMdcOgAttribute(pair, block)
}
