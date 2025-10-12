package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.resources.Opphevstans
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.bekreftTilgangTilPerson
import no.nav.helse.spesialist.application.KøetMeldingPubliserer
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PostOpphevStansBehandler : PostBehandler<Opphevstans, OpphevStansRequest, Boolean> {
    override fun behandle(
        resource: Opphevstans,
        request: OpphevStansRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        meldingsKø: KøetMeldingPubliserer,
    ): RestResponse<Boolean> {
        val fødselsnummer = request.fodselsnummer
        bekreftTilgangTilPerson(
            fødselsnummer = fødselsnummer,
            saksbehandler = saksbehandler,
            tilgangsgrupper = tilgangsgrupper,
            transaksjon = transaksjon,
            feilSupplier = ::HttpForbidden,
        )

        transaksjon.stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer = fødselsnummer)
        transaksjon.notatDao.lagreForOppgaveId(
            oppgaveId =
                transaksjon.oppgaveDao.finnOppgaveId(fødselsnummer = fødselsnummer)
                    ?: transaksjon.oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer = fødselsnummer),
            tekst = request.begrunnelse,
            saksbehandlerOid = saksbehandler.id().value,
            notatType = NotatType.OpphevStans,
            dialogRef = transaksjon.dialogDao.lagre(),
        )

        return RestResponse.ok(true)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Stans av automatisering")
            operationId = operationIdBasertPåKlassenavn()
            request {
                body<OpphevStansRequest>()
            }
            response {
                code(HttpStatusCode.OK) {
                    description = "Alltid true - henger igjen fra at Apollo ikke tåler å ikke få noe i body"
                    body<Boolean>()
                }
            }
        }
    }
}
