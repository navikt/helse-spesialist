package no.nav.helse.spesialist.api.rest

import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.ktor.http.HttpStatusCode
import no.nav.helse.spesialist.api.rest.resources.Opphevstans
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgang

class PostOpphevStansBehandler : PostBehandler<Opphevstans, ApiOpphevStansRequest, Unit, ApiPostOpphevStansErrorCode> {
    override val påkrevdTilgang = Tilgang.Skriv

    override fun behandle(
        resource: Opphevstans,
        request: ApiOpphevStansRequest,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostOpphevStansErrorCode> =
        kallKontekst.medPerson(
            identitetsnummer = Identitetsnummer.fraString(request.fodselsnummer),
            personIkkeFunnet = { ApiPostOpphevStansErrorCode.PERSON_IKKE_FUNNET },
            manglerTilgangTilPerson = { ApiPostOpphevStansErrorCode.MANGLER_TILGANG_TIL_PERSON },
        ) { person ->
            behandleForPerson(request, person, kallKontekst)
        }

    private fun behandleForPerson(
        request: ApiOpphevStansRequest,
        person: Person,
        kallKontekst: KallKontekst,
    ): RestResponse<Unit, ApiPostOpphevStansErrorCode> {
        kallKontekst.transaksjon.stansAutomatiskBehandlingDao.lagreFraSpeil(fødselsnummer = person.id.value)
        kallKontekst.transaksjon.notatDao.lagreForOppgaveId(
            oppgaveId =
                kallKontekst.transaksjon.oppgaveDao.finnOppgaveId(fødselsnummer = person.id.value)
                    ?: kallKontekst.transaksjon.oppgaveDao.finnOppgaveIdUansettStatus(fødselsnummer = person.id.value),
            tekst = request.begrunnelse,
            saksbehandlerOid = kallKontekst.saksbehandler.id.value,
            notatType = NotatType.OpphevStans,
            dialogRef = kallKontekst.transaksjon.dialogDao.lagre(),
        )

        loggInfo("Lagret stans av automatisk behandling for person")

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
    PERSON_IKKE_FUNNET("Person ikke funnet", HttpStatusCode.BadRequest),
    MANGLER_TILGANG_TIL_PERSON("Mangler tilgang til person", HttpStatusCode.Forbidden),
}
