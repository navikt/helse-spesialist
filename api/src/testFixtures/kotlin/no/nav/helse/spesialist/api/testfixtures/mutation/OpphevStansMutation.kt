package no.nav.helse.spesialist.api.testfixtures.mutation

fun opphevStansMutation(fødselsnummer: String, begrunnelse: String) = asGQL("""
    mutation OpphevStans {
        opphevStans(fodselsnummer: "$fødselsnummer", begrunnelse: "$begrunnelse")
    }
""")
