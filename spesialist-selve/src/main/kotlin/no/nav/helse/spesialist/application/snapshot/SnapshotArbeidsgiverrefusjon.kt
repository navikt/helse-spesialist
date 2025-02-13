package no.nav.helse.spesialist.application.snapshot

data class SnapshotArbeidsgiverrefusjon(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<SnapshotRefusjonselement>,
)
