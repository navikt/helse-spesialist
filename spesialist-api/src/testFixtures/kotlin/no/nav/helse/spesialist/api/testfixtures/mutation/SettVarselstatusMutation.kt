package no.nav.helse.spesialist.api.testfixtures.mutation

import java.util.UUID

fun settVarselstatusMutation(behandlingId: UUID?, definisjonId: UUID? = null, saksbehandlerOid: UUID, varselkode: String) = asGQL(
    """
    mutation SettVarselstatus {
        settVarselstatus(
            behandlingIdString: "$behandlingId", 
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
"""
)
