package no.nav.helse.modell.command.ny

import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language

abstract class NyMacroCommand() : NyCommand {
    internal abstract val commands: List<NyCommand>

    override fun execute(session: Session): NyCommand.Resultat {
        return commands.execute(session)
    }

    override fun resume(session: Session): NyCommand.Resultat {
        val currentCommandType = session.finnCurrentCommandType(this.type)
        val (first, tail) = commands.dropWhile { it.type != currentCommandType }.split()
        val resultat = first.resume(session)
        session.oppdaterCommandResultat(resultat, first, this.type)
        return if (resultat.suspends) {
            resultat
        } else {
            tail.execute(session)
        }
    }

    private fun List<NyCommand>.execute(session: Session) =
        asSequence()
            .map { subCommand -> subCommand.execute(session) to subCommand }
            .onEach { (resultat, command) ->
                persister(
                    session = session,
                    subCommand = command,
                    type = this@NyMacroCommand.type,
                    resultat = resultat
                )
            }
            .firstOrNull { (resultat, _) -> resultat.suspends }
            ?.let { (resultat, _) -> resultat }
            ?: NyCommand.Resultat.Ok

    private fun persister(session: Session, subCommand: NyCommand, type: String, resultat: NyCommand.Resultat) {
        session.persisterCommand(subCommand, type, resultat)
    }
}

fun <T> List<T>.split() = Pair(first(), drop(1))

fun Session.persisterCommand(subCommand: NyCommand, type: String, resultat: NyCommand.Resultat): Long? {
    @Language("PostgreSQL")
    val query =
        """INSERT INTO command (macro_type, command_type, resultat) VALUES (:macro_type, :command_type, :resultat)"""
    return run(
        queryOf(
            query,
            mapOf(
                "macro_type" to type,
                "command_type" to subCommand.type,
                "resultat" to resultat.name
            )
        ).asUpdateAndReturnGeneratedKey
    )
}

fun Session.oppdaterCommandResultat(resultat: NyCommand.Resultat, subCommand: NyCommand, macroType: String): Long? {
    @Language("PostgreSQL")
    val query =
        """UPDATE command SET resultat=:resultat WHERE macro_type=:macro_type AND command_type=:command_type"""
    return run(
        queryOf(
            query,
            mapOf(
                "resultat" to resultat.name,
                "macro_type" to macroType,
                "command_type" to subCommand.type
            )
        )
            .asUpdateAndReturnGeneratedKey
    )
}

fun Session.finnCurrentCommandType(macroType: String): String? {
    @Language("PostgreSQL")
    val query =
        """SELECT command_type FROM command WHERE macro_type=:macro_type AND resultat != 'Ok' ORDER BY id DESC LIMIT 1"""
    return run(queryOf(query, mapOf("macro_type" to macroType))
        .map { it.string("command_type") }
        .asSingle
    )
}
