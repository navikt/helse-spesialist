package no.nav.helse.spesialist.api.testfixtures

import no.nav.helse.spesialist.api.saksbehandler.SaksbehandlerFraApi
import no.nav.helse.spesialist.domain.testfixtures.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlernavn
import java.util.UUID

fun lagSaksbehandlerFraApi(
    saksbehandlernavn: String = lagSaksbehandlernavn(),
    oid: UUID = UUID.randomUUID(),
    epostadresse: String = lagEpostadresseFraFulltNavn(saksbehandlernavn),
    ident: String = lagSaksbehandlerident(),
    grupper: List<UUID> = listOf(UUID.randomUUID()),
): SaksbehandlerFraApi {
    return SaksbehandlerFraApi(
        oid = oid,
        navn = saksbehandlernavn,
        epost = epostadresse,
        ident = ident,
        grupper = grupper,
        tilgangsgrupper = emptySet()
    )
}
