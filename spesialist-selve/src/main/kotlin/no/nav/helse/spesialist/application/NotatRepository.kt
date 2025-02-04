package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.modell.Notat
import java.util.UUID

interface NotatRepository {
    fun lagre(notat: Notat)

    fun finn(notatId: Int): Notat?

    fun finnAlleForVedtaksperiode(vedtaksperiodeId: UUID): List<Notat>
}
