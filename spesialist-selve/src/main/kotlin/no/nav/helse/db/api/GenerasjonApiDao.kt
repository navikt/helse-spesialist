package no.nav.helse.db.api

import no.nav.helse.spesialist.api.varsel.Varsel
import no.nav.helse.spesialist.api.vedtak.Vedtaksperiode
import java.util.UUID

interface GenerasjonApiDao {
    fun gjeldendeGenerasjonFor(oppgaveId: Long): Vedtaksperiode

    fun gjeldendeGenerasjonerForPerson(oppgaveId: Long): Set<Vedtaksperiode>

    fun gjeldendeGenerasjonFor(
        oppgaveId: Long,
        varselGetter: (generasjonId: UUID) -> Set<Varsel>,
    ): Vedtaksperiode

    fun gjeldendeGenerasjonerForPerson(
        oppgaveId: Long,
        varselGetter: (generasjonId: UUID) -> Set<Varsel>,
    ): Set<Vedtaksperiode>
}
