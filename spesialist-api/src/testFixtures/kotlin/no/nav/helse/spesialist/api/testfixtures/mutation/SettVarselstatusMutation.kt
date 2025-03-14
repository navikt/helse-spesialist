package no.nav.helse.spesialist.api.testfixtures.mutation

import java.util.UUID

fun settVarselstatusMutation(generasjonId: UUID?, definisjonId: UUID? = null, saksbehandlerOid: UUID, varselkode: String) = asGQL("""
    mutation SettVarselstatus {
        settVarselstatus(
            generasjonIdString: "$generasjonId", 
            definisjonIdString: ${definisjonId?.let { """"$it"""" }},
            varselkode: "$varselkode", 
            ident: "$saksbehandlerOid" 
        ) {
            kode
            vurdering {
                status
            }
        }
    }
""")
