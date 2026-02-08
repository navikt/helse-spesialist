package no.nav.helse.spesialist.api.rest

import io.ktor.server.routing.RoutingCall
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.medMdcOgAttribute
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.PersonPseudoId
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
import no.nav.helse.spesialist.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class KallKontekst(
    val saksbehandler: Saksbehandler,
    val brukerroller: Set<Brukerrolle>,
    val tilganger: Set<Tilgang>,
    val transaksjon: SessionContext,
    val outbox: Outbox,
    private val ktorCall: RoutingCall,
) {
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
            val identitetsnummer = transaksjon.personPseudoIdDao.hentIdentitetsnummer(personPseudoId)

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

            if (!person.kanSeesAvSaksbehandlerMedGrupper(brukerroller)) {
                loggWarn("Saksbehandler har ikke tilgang til personen", "identitetsnummer" to identitetsnummer)
                return@medMdcOgAttribute RestResponse.Error(manglerTilgangTilPerson())
            }

            val oppgaveId = transaksjon.oppgaveDao.finnOppgaveId(identitetsnummer.value)

            // Best effort for å finne ut om saksbehandler har tilgang til oppgaven som gjelder
            // Litt vanskelig å få pent så lenge vi har dynamisk resolving av resten, og tilsynelatende "mange" oppgaver
            val harTilgangTilOppgave =
                oppgaveId?.let { oppgaveId ->
                    transaksjon.oppgaveRepository
                        .finn(oppgaveId)
                        ?.kanSeesAv(brukerroller)
                } ?: true

            if (!harTilgangTilOppgave) {
                loggWarn("Saksbehandler har ikke tilgang til aktiv oppgave på personen", "identitetsnummer" to identitetsnummer)
                return@medMdcOgAttribute RestResponse.Error(manglerTilgangTilPerson())
            }

            block(person)
        }

    fun <T> medMdcOgAttribute(
        pair: Pair<MdcKey, String>,
        block: () -> T,
    ): T = ktorCall.medMdcOgAttribute(pair, block)
}
