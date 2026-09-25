package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotPerson(
    val arbeidsgivere: List<SnapshotArbeidsgiver>,
    val dodsdato: LocalDate?,
    val fodselsnummer: String,
    val vilkarsgrunnlag: List<SnapshotVilkarsgrunnlag>,
)
