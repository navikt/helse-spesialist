package no.nav.helse.vedtaksperiode

data class VedtaksperiodeDto(
    val fødselsnummer: String,
    val aktørId: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val arbeidsgiverRef: Int,
    val speilSnapshotRef: Int,
    val infotrygdutbetalingerRef: Int?
)
