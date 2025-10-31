package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.resources.Opphevstans
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.harTilgangTilPerson
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PostOpphevStansBehandler : PostBehandler<Opphevstans, ApiOpphevStansRequest, Boolean, ApiPostOpphevStansErrorCode> {
    override fun behandle(
        resource: Opphevstans,
        request: ApiOpphevStansRequest,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        outbox: Outbox,
    ): RestResponse<Boolean, ApiPostOpphevStansErrorCode> {
        val fødselsnummer = request.fodselsnummer
        if (!harTilgangTilPerson(
                fødselsnummer = fødselsnummer,
                saksbehandler = saksbehandler,
                tilgangsgrupper = tilgangsgrupper,
                transaksjon = transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostOpphevStansErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

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

        return RestResponse.OK(true)
    }

    override fun openApi(config: RouteConfig) {
        with(config) {
            tags = setOf("Stans av automatisering")
        }
    }
}

enum class ApiPostOpphevStansErrorCode(
    override val title: String,
    override val statusCode: HttpStatusCode,
) : ApiErrorCode {
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
