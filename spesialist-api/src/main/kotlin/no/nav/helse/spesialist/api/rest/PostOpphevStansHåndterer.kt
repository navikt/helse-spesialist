package no.nav.helse.spesialist.api.rest

import io.ktor.http.Parameters
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.bekreftTilgangTilPerson
import no.nav.helse.spesialist.application.KøetMeldingPubliserer
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe
import kotlin.reflect.typeOf

class PostOpphevStansHåndterer : PostHåndterer<Unit, PostOpphevStansHåndterer.RequestBody, Boolean> {
    override val urlPath: String = "opphevstans"

    data class RequestBody(
        val fodselsnummer: String,
        val begrunnelse: String,
    )

    override fun extractParametre(
        pathParameters: Parameters,
        queryParameters: Parameters,
    ) = Unit

    override fun håndter(
        urlParametre: Unit,
        requestBody: RequestBody,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        meldingsKø: KøetMeldingPubliserer,
    ): RestResponse<Boolean> {
        val fødselsnummer = requestBody.fodselsnummer
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
            tekst = requestBody.begrunnelse,
            saksbehandlerOid = saksbehandler.id().value,
            notatType = NotatType.OpphevStans,
            dialogRef = transaksjon.dialogDao.lagre(),
        )

        return RestResponse.ok(true)
    }

    override val urlParametersClass = Unit::class

    override val requestBodyType = typeOf<RequestBody>()

    override val responseBodyType = typeOf<Boolean>()
}
