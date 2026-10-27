package no.nav.helse.spesialist.domain.andreytelser

import no.nav.helse.spesialist.domain.NAVIdent
import java.time.Instant
import java.util.UUID

data class GraderteAndreYtelserEndretEvent(
    val versjon: UUID,
    val tidspunkt: Instant,
    val utførtAvSaksbehandlerIdent: NAVIdent,
    val notatTilBeslutter: String,
    val dager: List<GraderteAndreYtelserDag>,
)
