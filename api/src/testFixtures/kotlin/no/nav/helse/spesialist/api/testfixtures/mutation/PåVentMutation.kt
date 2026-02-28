package no.nav.helse.spesialist.api.testfixtures.mutation

import java.time.LocalDate

fun leggPåVentMutation(
    oppgaveId: Long,
    notatTekst: String,
    frist: LocalDate,
    tildeling: Boolean,
    arsaker: Map<String, String>? = null,
) = asGQL("""
    mutation LeggPaVent {
        leggPaVent(
            notatTekst: "$notatTekst",
            frist: "$frist",
            oppgaveId: "$oppgaveId",
            tildeling: $tildeling,
            arsaker: ${arsaker?.entries?.joinToString(prefix = "{ ", postfix = " }") { "${it.key}: \"${it.value}\""} }
        ) {
            oid
        }
    }
""")

fun fjernPåVentMutation(oppgaveId: Long) = asGQL("""
    mutation {
        fjernPaVent(
            oppgaveId: "$oppgaveId",
        )
    }
""")
