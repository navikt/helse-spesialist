package no.nav.helse.modell.command.ny

import kotliquery.Session
import kotliquery.queryOf
import org.intellij.lang.annotations.Language

class NyMacroCommand(
    private val commands: List<NyCommand>,
    override val type: String,
    private val id: Long
) : NyCommand {

    override fun execute(session: Session) = commands.execute(session)

    override fun resume(session: Session): NyCommand.Resultat {
        val currentCommandType = session.finnCurrentCommandType(this.id)
        val (first, tail) = commands.dropWhile { it.type != currentCommandType }.split()
        val resultat = first.resume(session).also {
            session.persisterCommand(type = first.type, parent = id)
        }
        return if (resultat.suspends) {
            resultat
        } else {
            tail.execute(session)
        }
    }

    private fun List<NyCommand>.execute(session: Session) =
        asSequence()
            .map { subCommand -> subCommand.execute(session) to subCommand }
            .onEach { (_, command) ->
                session.persisterCommand(
                    type = command.type,
                    parent = id
                )
            }
            .firstOrNull { (resultat, _) -> resultat.suspends }
            ?.let { (resultat, _) -> resultat }
            ?: NyCommand.Resultat.Ok
}

fun <T> List<T>.split() = Pair(first(), drop(1))

fun Session.persisterCommand(type: String, parent: Long?): Long? {
    @Language("PostgreSQL")
    val query =
        """
            INSERT INTO command (command_type, parent_ref)
            VALUES (:command_type, :parent)
        """
    return run(
        queryOf(
            query,
            mapOf(
                "command_type" to type,
                "parent" to parent
            )
        ).asUpdateAndReturnGeneratedKey
    )
}

fun Session.finnCurrentCommandType(id: Long): String? {
    @Language("PostgreSQL")
    val query =
        """SELECT command_type FROM command WHERE parent_ref=:id ORDER BY id DESC LIMIT 1"""
    return run(queryOf(query, mapOf("id" to id))
        .map { it.string("command_type") }
        .asSingle
    )
}
