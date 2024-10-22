package no.nav.helse.modell.risiko

import com.fasterxml.jackson.databind.JsonNode
import kotliquery.sessionOf
import no.nav.helse.db.RisikovurderingRepository
import no.nav.helse.db.TransactionalRisikovurderingDao
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

internal class RisikovurderingDao(val dataSource: DataSource) : RisikovurderingRepository {
    override fun lagre(
        vedtaksperiodeId: UUID,
        kanGodkjennesAutomatisk: Boolean,
        kreverSupersaksbehandler: Boolean,
        data: JsonNode,
        opprettet: LocalDateTime,
    ) = sessionOf(dataSource).use { session ->
        TransactionalRisikovurderingDao(session).lagre(
            vedtaksperiodeId,
            kanGodkjennesAutomatisk,
            kreverSupersaksbehandler,
            data,
            opprettet,
        )
    }

    override fun hentRisikovurdering(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            TransactionalRisikovurderingDao(session).hentRisikovurdering(vedtaksperiodeId)
        }

    override fun kreverSupersaksbehandler(vedtaksperiodeId: UUID) =
        sessionOf(dataSource).use { session ->
            TransactionalRisikovurderingDao(session).kreverSupersaksbehandler(vedtaksperiodeId)
        }
}
