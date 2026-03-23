package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Opptegnelse
import no.nav.helse.spesialist.domain.Sekvensnummer

interface OpptegnelseRepository {
    fun lagre(opptegnelse: Opptegnelse)

    fun finnAlleForPersonEtter(
        opptegnelseId: Sekvensnummer,
        personIdentitetsnummer: Identitetsnummer,
    ): List<Opptegnelse>

    fun finnNyesteSekvensnummer(): Sekvensnummer
}
