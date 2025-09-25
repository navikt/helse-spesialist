package no.nav.helse.spesialist.api.rest

import io.ktor.http.HttpStatusCode
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.api.rest.tilkommeninntekt.bekreftTilgangTilPerson
import no.nav.helse.spesialist.application.KøetMeldingPubliserer
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.tilgangskontroll.Tilgangsgruppe

class PostOpphevStansHåndterer : PostHåndterer<Unit, PostOpphevStansHåndterer.RequestBody, HttpStatusCode> {
    data class RequestBody(
        val fødselsnummer: String,
        val begrunnelse: String,
    )

    override fun håndter(
        urlParametre: Unit,
        requestBody: RequestBody,
        saksbehandler: Saksbehandler,
        tilgangsgrupper: Set<Tilgangsgruppe>,
        transaksjon: SessionContext,
        meldingsKø: KøetMeldingPubliserer,
    ): HttpStatusCode {
        val fødselsnummer = requestBody.fødselsnummer
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

        return HttpStatusCode.NoContent
    }
}
