package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverId

interface ArbeidsgiverRepository {
    fun lagre(arbeidsgiver: Arbeidsgiver)

    fun finn(id: ArbeidsgiverId): Arbeidsgiver?

    fun finnForIdentifikator(identifikator: Arbeidsgiver.Identifikator): Arbeidsgiver?

    fun finnAlleForIdentifikatorer(identifikatorer: Set<Arbeidsgiver.Identifikator>): List<Arbeidsgiver>
}
