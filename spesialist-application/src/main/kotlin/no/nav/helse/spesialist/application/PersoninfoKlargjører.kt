package no.nav.helse.spesialist.application

import no.nav.helse.spesialist.domain.Person

/**
 * Sørger for at en [Person] har personinfo, ved å hente det synkront med [PersoninfoHenter] dersom det mangler.
 */
class PersoninfoKlargjører(
    private val personinfoHenter: PersoninfoHenter,
) {
    fun klargjør(person: Person): KlargjøringResultat {
        if (person.info != null) return KlargjøringResultat.Klargjort

        val personinfo =
            try {
                personinfoHenter.hentPersoninfo(person.id)
            } catch (e: Exception) {
                return KlargjøringResultat.OppslagFeilet(e)
            } ?: return KlargjøringResultat.IkkeFunnet

        person.oppdaterInfo(personinfo)
        return KlargjøringResultat.Klargjort
    }

    sealed interface KlargjøringResultat {
        data object Klargjort : KlargjøringResultat

        data object IkkeFunnet : KlargjøringResultat

        data class OppslagFeilet(
            val feil: Exception,
        ) : KlargjøringResultat
    }
}
