package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.bootstrap.EnvironmentToggles
import no.nav.helse.db.BehandlingRepository
import no.nav.helse.db.SessionContext
import no.nav.helse.modell.oppgave.Oppgave
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.spesialist.api.rest.resources.Vedtak
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.bekreftTilgangTilPerson
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PostFattVedtakBehandler(
    private val environmentToggles: EnvironmentToggles,
) : PostBehandler<Vedtak.Id.Fatt, Unit, Boolean> {
    override fun behandle(
        resource: Vedtak.Id.Fatt,
        request: Unit,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Boolean> {
        val spleisBehandlingId = SpleisBehandlingId(resource.parent.behandlingId)
        val behandling =
            transaksjon.behandlingRepository.finn(spleisBehandlingId)
                ?: throw HttpNotFound("Fant ikke behandling med behandlingId ${resource.parent.behandlingId}")
        val vedtaksperiode =
            transaksjon.vedtaksperiodeRepository.finn(behandling.vedtaksperiodeId)
                ?: throw HttpNotFound("Fant ikke vedtaksperiode med id ${behandling.vedtaksperiodeId.value}")
        val fødselsnummer = vedtaksperiode.fødselsnummer
        bekreftTilgangTilPerson(
            fødselsnummer = fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )
        val oppgave = transaksjon.oppgaveRepository.finn(spleisBehandlingId)

        if (oppgave == null) {
            throw HttpBadRequest(OPPGAVE_IKKE_FUNNET)
        } else if (oppgave.tilstand != Oppgave.AvventerSaksbehandler) {
            throw HttpBadRequest(OPPGAVE_FEIL_TILSTAND)
        }

        val totrinnsvurdering = transaksjon.totrinnsvurderingRepository.finnAktivForPerson(fødselsnummer)
        if (totrinnsvurdering == null) {
            behandling.fattVedtak(transaksjon.behandlingRepository, fødselsnummer)
        } else {
            totrinnsvurdering.godkjenn(saksbehandler, tilgangsgrupper)
            transaksjon.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        }

        return RestResponse(HttpStatusCode.OK, false)
    }

    private fun Behandling.fattVedtak(
        behandlingRepository: BehandlingRepository,
        fødselsnummer: String,
    ) {
        val behandlingerSomMåSeesUnderEtt =
            behandlingRepository
                .finnAndreBehandlingerISykefraværstilfelle(this, fødselsnummer)
                .filterNot { it.fom > tom }
                .plus(this)
    }

    private fun Totrinnsvurdering.godkjenn(
        beslutter: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
    ) {
        if (Tilgangsgruppe.BESLUTTER !in tilgangsgrupper && !environmentToggles.kanGodkjenneUtenBesluttertilgang) {
            throw HttpForbidden(SAKSBEHANDLER_MANGLER_BESLUTTERTILGANG)
        }
        if (this.saksbehandler?.value == beslutter.id().value && !environmentToggles.kanBeslutteEgneSaker) {
            throw HttpForbidden(SAKSBEHANDLER_KAN_IKKE_BESLUTTE_EGEN_OPPGAVE)
        }
        settBeslutter(beslutter.id())
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = listOf("Vedtak")
            operationId = operationIdBasertPåKlassenavn()
            response {
                code(HttpStatusCode.OK) {
                    body<Boolean>()
                }
            }
        }
    }

    companion object {
        const val OPPGAVE_IKKE_FUNNET = "Fant ikke oppgave."
        const val OPPGAVE_FEIL_TILSTAND = "Oppgaven er i feil tilstand."
        const val SAKSBEHANDLER_MANGLER_BESLUTTERTILGANG = "Mangler besluttertilgang"
        const val SAKSBEHANDLER_KAN_IKKE_BESLUTTE_EGEN_OPPGAVE = "Kan ikke beslutte egen oppgave"
    }
}
