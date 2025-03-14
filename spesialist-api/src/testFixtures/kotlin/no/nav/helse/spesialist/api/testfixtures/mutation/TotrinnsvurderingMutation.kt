package no.nav.helse.spesialist.api.testfixtures.mutation

fun sendIReturMutation(oppgaveId: Long, notatTekst: String) = asGQL("""
    mutation TotrinnsvurderingMutation {
        sendIRetur(oppgavereferanse: "$oppgaveId", notatTekst: "$notatTekst")
    }
""")

fun sendTilbeslutterMutation(oppgaveId: Long) = asGQL("""
    mutation TotrinnsvurderingMutation {
        sendTilGodkjenningV2(oppgavereferanse: "$oppgaveId")
    }
""")
