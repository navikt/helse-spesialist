package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId

interface GraderteAndreYtelserRepository {
    fun finnAlleForIdentitetsnummer(identitetsnummer: Identitetsnummer): List<GraderteAndreYtelser>

    fun finn(id: GraderteAndreYtelserId): GraderteAndreYtelser?

    fun lagre(graderteAndreYtelser: GraderteAndreYtelser)
}
