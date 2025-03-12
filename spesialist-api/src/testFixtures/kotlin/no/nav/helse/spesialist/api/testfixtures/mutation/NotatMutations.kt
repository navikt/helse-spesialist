package no.nav.helse.spesialist.api.testfixtures.mutation

import no.nav.helse.spesialist.api.graphql.schema.ApiNotatType
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.KommentarId
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import java.util.UUID

fun leggTilNotatMutation(
    tekst: String,
    type: ApiNotatType,
    vedtaksperiodeId: UUID,
    saksbehandlerOid: SaksbehandlerOid
): String = asGQL(
    """
        mutation LeggTilNotat {
            leggTilNotat(
                tekst: "$tekst",
                type: $type,
                vedtaksperiodeId: "$vedtaksperiodeId",
                saksbehandlerOid: "${saksbehandlerOid.value}"
            ) {
                id,
                dialogRef,
                tekst,
                opprettet,
                saksbehandlerOid,
                saksbehandlerNavn,
                saksbehandlerEpost,
                saksbehandlerIdent,
                vedtaksperiodeId,
                feilregistrert,
                feilregistrert_tidspunkt,
                type,
                kommentarer {
                    id,
                    tekst,
                    opprettet,
                    saksbehandlerident,
                    feilregistrert_tidspunkt,
                }
            }
        }
    """
)

fun feilregistrerNotatMutation(notatId: NotatId): String = asGQL(
    """
        mutation FeilregistrerNotat {
            feilregistrerNotat(id: ${notatId.value}) { 
                id,
                dialogRef,
                tekst,
                opprettet,
                saksbehandlerOid,
                saksbehandlerNavn,
                saksbehandlerEpost,
                saksbehandlerIdent,
                vedtaksperiodeId,
                feilregistrert,
                feilregistrert_tidspunkt,
                type,
                kommentarer {
                    id,
                    tekst,
                    opprettet,
                    saksbehandlerident,
                    feilregistrert_tidspunkt,
                }
            }
        }
    """
)

fun feilregistrerKommentarMutation(kommentarId: KommentarId): String = asGQL(
    """
            mutation FeilregistrerKommentar {
                feilregistrerKommentar(id: ${kommentarId.value}) {
                    id,
                    tekst,
                    opprettet,
                    saksbehandlerident,
                    feilregistrert_tidspunkt,
                }
            }
        """
)

fun leggTilKommentarMutation(dialogRef: DialogId, tekst: String, saksbehandlerIdent: String): String = asGQL(
    """
        mutation LeggTilKommentar {
            leggTilKommentar(dialogRef: ${dialogRef.value}, tekst: "$tekst", saksbehandlerident: "$saksbehandlerIdent") {
                id,
                tekst,
                opprettet,
                saksbehandlerident,
                feilregistrert_tidspunkt,
            }
        }
    """
)