package no.nav.helse.spesialist.application

import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId

interface OverstyringRepository {
    fun lagre(
        overstyringer: List<Overstyring>,
        totrinnsvurderingId: TotrinnsvurderingId? = null,
    )

    fun finnAktive(
        fødselsnummer: String,
        totrinnsvurderingId: TotrinnsvurderingId,
    ): List<Overstyring>

    @Deprecated("Ny totrinnsløype bruker totrinnsvurderingId til å finne overstyringer")
    fun finnAktive(fødselsnummer: String): List<Overstyring>
}
