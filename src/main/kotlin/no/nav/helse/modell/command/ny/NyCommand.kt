package no.nav.helse.modell.command.ny

import kotliquery.Session

interface NyCommand {
    val type: String
    fun execute(session: Session): Resultat
    fun resume(session: Session): Resultat = error("Resume er ikke implementert for denne commanden")

    enum class Resultat(val suspends: Boolean) {
        Ok(suspends = false),
        AvventerSystem(suspends = true),
        AvventerSaksbehandler(suspends = true),
        Failure(suspends = true)
    }
}
