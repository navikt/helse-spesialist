package no.nav.helse.person

data class PersonMetadataDto(
    val fødselsnummer: String,
    val aktørId: String,
    val personinfo: PersoninfoApiDto,
    val arbeidsgiverRef: Long,
    val speilSnapshotRef: Int,
    val infotrygdutbetalingerRef: Int?
)
