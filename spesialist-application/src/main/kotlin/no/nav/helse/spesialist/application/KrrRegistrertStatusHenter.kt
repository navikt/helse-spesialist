package no.nav.helse.spesialist.application

fun interface KrrRegistrertStatusHenter {
    suspend fun hentForPerson(fødselsnummer: String): KrrRegistrertStatus

    enum class KrrRegistrertStatus {
        RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING,
        IKKE_RESERVERT_MOT_DIGITAL_KOMMUNIKASJON_ELLER_VARSLING,
        IKKE_REGISTRERT_I_KRR,
    }
}
