package no.nav.helse.db.api

import no.nav.helse.spesialist.api.graphql.schema.PaVent
import java.util.UUID

interface PåVentApiDao {
    fun hentAktivPåVent(vedtaksperiodeId: UUID): PaVent?
}
