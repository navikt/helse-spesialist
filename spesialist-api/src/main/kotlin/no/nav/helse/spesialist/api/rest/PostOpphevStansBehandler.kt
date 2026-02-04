package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.resources.Opphevstans
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class PostOpphevStansBehandler : PostBehandler<Opphevstans, ApiOpphevStansRequest, Unit, ApiPostOpphevStansErrorCode> {
    override val påkrevdeTilganger: Set<Tilgang> = setOf(Tilgang.Skriv)

    override fun behandle(
        resource: Opphevstans,
        request: ApiOpphevStansRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostOpphevStansErrorCode> {
        val fødselsnummer = request.fodselsnummer
        if (!kallKontekst.saksbehandler.harTilgangTilPerson(
                identitetsnummer = Identitetsnummer.fraString(identitetsnummer = fødselsnummer),
                brukerroller = kallKontekst.brukerroller,
                transaksjon = kallKontekst.transaksjon,
            )
        ) {
            return RestResponse.Error(ApiPostOpphevStansErrorCode.MANGLER_TILGANG_TIL_PERSON)
        }

        kallKontekst.transaksjon.stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer = fødselsnummer)
        kallKontekst.transaksjon.notatDao.lagreForOppgaveId(
            oppgaveId =
                kallKontekst.transaksjon.oppgaveDao.finnOppgaveId(fødselsnummer = fødselsnummer)
                    ?: kallKontekst.transaksjon.oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer = fødselsnummer),
            tekst = request.begrunnelse,
            saksbehandlerOid = kallKontekst.saksbehandler.id.value,
            notatType = NotatType.OpphevStans,
            dialogRef = kallKontekst.transaksjon.dialogDao.lagre(),
        )

        return RestResponse.NoContent()
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
