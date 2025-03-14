package no.nav.helse.spesialist.api.testfixtures.mutation

fun abonnerMutation(personidentifikator: String) = asGQL("""
    mutation Abonner {
        opprettAbonnement(personidentifikator: "$personidentifikator")
    }
""")
