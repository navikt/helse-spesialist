package no.nav.helse.spesialist.db.repository

import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.saksbehandlerhandling.HandlingType
import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling
import no.nav.helse.spesialist.domain.saksbehandlerhandling.SaksbehandlerhandlingId
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PgSaksbehandlerhandlingRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = PgSaksbehandlerhandlingRepository(session)

    @Test
    fun `lagrer saksbehandlerhandling med totrinnsvurdering`() {
        val person = opprettPerson()
        val totrinnsvurderingId = opprettTotrinnsvurdering(person)
        val behandlingId = UUID.randomUUID()
        val handling =
            lagSaksbehandlerhandling(
                identitetsnummer = person.id,
                totrinnsvurderingId = totrinnsvurderingId,
                skjæringstidspunkt = LocalDate.of(2018, 1, 1),
                behandlingId = SpleisBehandlingId(behandlingId),
            )

        repository.lagre(handling)

        val rader = hentRader(person.id)
        assertEquals(1, rader.size)
        val rad = rader.single()
        assertEquals(handling.id.value, rad.id)
        assertEquals(handling.handlingId, rad.handlingId)
        assertEquals(handling.handlingType.name, rad.handlingType)
        assertEquals(handling.saksbehandlerIdent, rad.saksbehandlerIdent)
        assertEquals(handling.tidspunkt.truncatedTo(ChronoUnit.MILLIS), rad.tidspunkt.truncatedTo(ChronoUnit.MILLIS))
        assertEquals(totrinnsvurderingId.value, rad.totrinnsvurderingId)
        assertEquals(LocalDate.of(2018, 1, 1), rad.skjæringstidspunkt)
        assertEquals(behandlingId, rad.behandlingId)
    }

    @Test
    fun `lagrer saksbehandlerhandling uten valgfrie felter`() {
        val person = opprettPerson()
        val handling = lagSaksbehandlerhandling(identitetsnummer = person.id)

        repository.lagre(handling)

        val rad = hentRader(person.id).single()
        assertNull(rad.totrinnsvurderingId)
        assertNull(rad.skjæringstidspunkt)
        assertNull(rad.behandlingId)
    }

    @Test
    fun `lagrer ikke samme handling to ganger`() {
        val person = opprettPerson()
        val handlingId = UUID.randomUUID()

        repository.lagre(lagSaksbehandlerhandling(identitetsnummer = person.id, handlingId = handlingId))
        repository.lagre(lagSaksbehandlerhandling(identitetsnummer = person.id, handlingId = handlingId))

        assertEquals(1, hentRader(person.id).size)
    }

    private fun lagSaksbehandlerhandling(
        identitetsnummer: Identitetsnummer,
        handlingId: UUID = UUID.randomUUID(),
        totrinnsvurderingId: TotrinnsvurderingId? = null,
        skjæringstidspunkt: LocalDate? = null,
        behandlingId: SpleisBehandlingId? = null,
    ) = Saksbehandlerhandling.ny(
        id = SaksbehandlerhandlingId(UUID.randomUUID()),
        identitetsnummer = identitetsnummer,
        handlingId = handlingId,
        handlingType = HandlingType.MANUELT_VURDERT_OPPTJENING,
        saksbehandlerIdent = "A123456",
        tidspunkt = Instant.now(),
        skjæringstidspunkt = skjæringstidspunkt,
        behandlingId = behandlingId,
        totrinnsvurderingId = totrinnsvurderingId,
    )

    private fun hentRader(identitetsnummer: Identitetsnummer) =
        dbQuery.list(
            "SELECT * FROM saksbehandlerhandling WHERE identitetsnummer = :identitetsnummer",
            "identitetsnummer" to identitetsnummer.value,
        ) {
            Rad(
                id = it.uuid("id"),
                handlingId = it.uuid("handling_id"),
                handlingType = it.string("handling_type"),
                saksbehandlerIdent = it.string("saksbehandler_ident"),
                tidspunkt = it.instant("tidspunkt"),
                totrinnsvurderingId = it.longOrNull("totrinnsvurdering_id"),
                skjæringstidspunkt = it.localDateOrNull("skjaeringstidspunkt"),
                behandlingId = it.uuidOrNull("behandling_id"),
            )
        }

    private data class Rad(
        val id: UUID,
        val handlingId: UUID,
        val handlingType: String,
        val saksbehandlerIdent: String,
        val tidspunkt: Instant,
        val totrinnsvurderingId: Long?,
        val skjæringstidspunkt: LocalDate?,
        val behandlingId: UUID?,
    )
}
