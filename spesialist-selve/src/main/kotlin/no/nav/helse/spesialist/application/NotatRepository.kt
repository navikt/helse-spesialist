package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.modell.Notat
import no.nav.helse.spesialist.modell.NotatId
import java.util.UUID

interface NotatRepository {
    fun lagre(notat: Notat)

    fun finn(id: NotatId): Notat?

    fun finnAlleForVedtaksperiode(vedtaksperiodeId: UUID): List<Notat>
}
