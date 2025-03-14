package no.nav.helse.spesialist.api.testfixtures.mutation

fun opprettTildelingMutation(oppgaveId: Long) = asGQL("""
    mutation OpprettTildeling {
        opprettTildeling(oppgaveId: "$oppgaveId") {
            navn, oid, epost
        }
    }
""")

fun fjernTildelingMutation(oppgaveId: Long) = asGQL("""
    mutation FjernTildeling {
        fjernTildeling(oppgaveId: "$oppgaveId")
    }
""")
