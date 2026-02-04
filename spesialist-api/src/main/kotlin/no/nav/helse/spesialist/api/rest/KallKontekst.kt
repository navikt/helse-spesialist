package no.nav.helse.spesialist.api.rest

import io.ktor.server.routing.RoutingCall
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.medMdcOgAttribute
import no.nav.helse.spesialist.api.rest.resources.Personer
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggWarn
import no.nav.helse.spesialist.application.logg.teamLogs
import no.nav.helse.spesialist.domain.Behandling
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
    fun <R, E : ApiErrorCode> medBehandling(
        spleisBehandlingId: SpleisBehandlingId,
        behandlingIkkeFunnet: E,
        vedtaksperiodeIkkeFunnet: E,
        personIkkeFunnet: E,
        manglerTilgangTilPerson: E,
        block: (Behandling, Vedtaksperiode, Person) -> RestResponse<R, E>,
    ): RestResponse<R, E> {
        val behandling =
            transaksjon.behandlingRepository.finn(spleisBehandlingId)
                ?: return RestResponse.Error(behandlingIkkeFunnet)

        return ktorCall.medMdcOgAttribute(MdcKey.SPLEIS_BEHANDLING_ID to spleisBehandlingId.value.toString()) {
            medVedtaksperiode(
                behandling.vedtaksperiodeId,
                vedtaksperiodeIkkeFunnet,
                personIkkeFunnet,
                manglerTilgangTilPerson,
            ) { vedtaksperiode, person -> block(behandling, vedtaksperiode, person) }
        }
    }

    fun <R, E : ApiErrorCode> medVedtaksperiode(
        vedtaksperiodeId: VedtaksperiodeId,
        vedtaksperiodeIkkeFunnet: E,
        personIkkeFunnet: E,
        manglerTilgangTilPerson: E,
        block: (Vedtaksperiode, Person) -> RestResponse<R, E>,
    ): RestResponse<R, E> {
        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(vedtaksperiodeId)
                ?: return RestResponse.Error(vedtaksperiodeIkkeFunnet)

        return ktorCall.medMdcOgAttribute(MdcKey.VEDTAKSPERIODE_ID to vedtaksperiodeId.value.toString()) {
            medPerson(
                Identitetsnummer.fraString(vedtaksperiode.fødselsnummer),
                personIkkeFunnet,
                manglerTilgangTilPerson,
            ) { person -> block(vedtaksperiode, person) }
        }
    }

    fun <R, E : ApiErrorCode> medPerson(
        personPseudoIdResource: Personer.PersonPseudoId,
        personIkkeFunnet: E,
        manglerTilgangTilPerson: E,
        block: (person: Person) -> RestResponse<R, E>,
    ): RestResponse<R, E> {
        val personPseudoId = personPseudoIdResource.pseudoId
        return ktorCall.medMdcOgAttribute(MdcKey.PERSON_PSEUDO_ID to personPseudoId) {
            medPerson(
                identitetsnummer =
                    transaksjon.personPseudoIdDao.hentIdentitetsnummer(
                        PersonPseudoId.fraString(personPseudoId),
                    ) ?: return@medMdcOgAttribute RestResponse.Error(personIkkeFunnet),
                personIkkeFunnet = personIkkeFunnet,
                manglerTilgangTilPerson = manglerTilgangTilPerson,
                block = block,
            )
        }
    }

    fun <R, E : ApiErrorCode> medPerson(
        identitetsnummer: Identitetsnummer,
        personIkkeFunnet: E,
        manglerTilgangTilPerson: E,
        block: (Person) -> RestResponse<R, E>,
    ): RestResponse<R, E> =
        ktorCall.medMdcOgAttribute(MdcKey.IDENTITETSNUMMER to identitetsnummer.value) {
            val person = transaksjon.personRepository.finn(identitetsnummer)

            if (person == null) {
                teamLogs.warn("Person med identitetsnummer $identitetsnummer ble ikke funnet")
                return@medMdcOgAttribute RestResponse.Error(personIkkeFunnet)
            }

            if (!person.kanSeesAvSaksbehandlerMedGrupper(brukerroller)) {
                teamLogs.warn("Saksbehandler har ikke tilgang til person med identitetsnummer $identitetsnummer")
                return@medMdcOgAttribute RestResponse.Error(manglerTilgangTilPerson)
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
                loggWarn("Saksbehandler mangler tilgang til aktiv oppgave på denne personen")
                return@medMdcOgAttribute RestResponse.Error(manglerTilgangTilPerson)
            }

            block(person)
        }
}
