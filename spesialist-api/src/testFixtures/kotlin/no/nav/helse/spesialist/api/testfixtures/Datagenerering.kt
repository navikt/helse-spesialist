package no.nav.helse.spesialist.api.testfixtures

import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.lagEpostadresseFraFulltNavn
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlerident
import no.nav.helse.spesialist.domain.testfixtures.lagSaksbehandlernavn
import java.util.UUID

fun lagSaksbehandler(
    saksbehandlernavn: String = lagSaksbehandlernavn(),
    oid: UUID = UUID.randomUUID(),
    epostadresse: String = lagEpostadresseFraFulltNavn(saksbehandlernavn),
    ident: String = lagSaksbehandlerident(),
): Saksbehandler {
    return Saksbehandler(
        id = SaksbehandlerOid(value = oid),
        navn = saksbehandlernavn,
        epost = epostadresse,
        ident = ident
    )
}
