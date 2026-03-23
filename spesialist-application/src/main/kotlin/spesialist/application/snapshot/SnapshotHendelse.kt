package no.nav.helse.spesialist.application.snapshot

import java.time.LocalDate
import java.time.LocalDateTime

sealed interface SnapshotHendelse

data class SnapshotInntektsmelding(
    val beregnetInntekt: Double,
    val id: String,
    val mottattDato: LocalDateTime,
    val type: SnapshotHendelsetype,
    val eksternDokumentId: String,
) : SnapshotHendelse

data class SnapshotSoknadArbeidsgiver(
    val fom: LocalDate,
    val id: String,
    val rapportertDato: LocalDateTime,
    val sendtArbeidsgiver: LocalDateTime,
    val tom: LocalDate,
    val type: SnapshotHendelsetype,
    val eksternDokumentId: String,
) : SnapshotHendelse

data class SnapshotSoknadNav(
    val fom: LocalDate,
    val id: String,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val tom: LocalDate,
    val type: SnapshotHendelsetype,
    val eksternDokumentId: String,
) : SnapshotHendelse

data class SnapshotSoknadArbeidsledig(
    val fom: LocalDate,
    val id: String,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val tom: LocalDate,
    val type: SnapshotHendelsetype,
    val eksternDokumentId: String,
) : SnapshotHendelse

data class SnapshotSoknadFrilans(
    val fom: LocalDate,
    val id: String,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val tom: LocalDate,
    val type: SnapshotHendelsetype,
    val eksternDokumentId: String,
) : SnapshotHendelse

data class SnapshotSoknadSelvstendig(
    val fom: LocalDate,
    val id: String,
    val rapportertDato: LocalDateTime,
    val sendtNav: LocalDateTime,
    val tom: LocalDate,
    val type: SnapshotHendelsetype,
    val eksternDokumentId: String,
) : SnapshotHendelse

data class SnapshotSykmelding(
    val fom: LocalDate,
    val id: String,
    val rapportertDato: LocalDateTime,
    val tom: LocalDate,
    val type: SnapshotHendelsetype,
) : SnapshotHendelse

data class SnapshotInntektFraAOrdningen(
    val id: String,
    val mottattDato: LocalDateTime,
    val type: SnapshotHendelsetype,
    val eksternDokumentId: String,
) : SnapshotHendelse

data object SnapshotUkjentHendelse : SnapshotHendelse
