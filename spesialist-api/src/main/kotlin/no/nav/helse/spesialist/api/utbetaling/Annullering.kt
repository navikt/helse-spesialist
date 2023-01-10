package no.nav.helse.spesialist.api.utbetaling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID
import no.nav.helse.spesialist.api.saksbehandler.Saksbehandler
import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerHendelse

@JsonIgnoreProperties
data class Annullering(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val fagsystemId: String,
    private val saksbehandlerIdent: String,
    private val begrunnelser: List<String> = emptyList(),
    private val kommentar: String?,
    private val saksbehandlerOid: UUID
) : SaksbehandlerHendelse {
    override fun tellernavn(): String = "annulleringer"
    override fun saksbehandlerOid(): UUID = saksbehandlerOid

    override fun håndter(saksbehandler: Saksbehandler) {
        saksbehandler.annuller(aktørId, fødselsnummer, organisasjonsnummer, fagsystemId, begrunnelser, kommentar)
    }
}

