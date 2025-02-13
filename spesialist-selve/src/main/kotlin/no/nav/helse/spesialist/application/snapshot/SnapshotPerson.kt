package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate

data class SnapshotPerson(
    val aktorId: String,
    val arbeidsgivere: List<SnapshotArbeidsgiver>,
    val dodsdato: LocalDate?,
    val fodselsnummer: String,
    val versjon: Int,
    val vilkarsgrunnlag: List<SnapshotVilkarsgrunnlag>,
)
