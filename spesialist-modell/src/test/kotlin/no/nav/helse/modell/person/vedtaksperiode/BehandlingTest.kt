package no.nav.helse.modell.person.vedtaksperiode

import io.mockk.mockk
import no.nav.helse.modell.desember
import no.nav.helse.modell.februar
import no.nav.helse.modell.januar
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.finnBehandlingForSpleisBehandling
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.finnBehandlingForVedtaksperiode
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.finnSisteBehandlingUtenSpleisBehandlingId
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.harMedlemskapsvarsel
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.harÅpenGosysOppgave
import no.nav.helse.modell.person.vedtaksperiode.Behandling.Companion.kreverSkjønnsfastsettelse
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.AKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.VURDERT
import no.nav.helse.modell.person.vedtaksperiode.Varselkode.SB_EX_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingTest {
    @Test
    fun `behandling ligger før dato`() {
        val behandling = Behandling(UUID.randomUUID(), UUID.randomUUID(), 1.januar, 31.januar, 1.januar)
        assertTrue(behandling.tilhører(31.januar))
        assertTrue(behandling.tilhører(1.februar))
        assertFalse(behandling.tilhører(1.januar))
        assertFalse(behandling.tilhører(31.desember(2017)))
    }

    @Test
    fun `behandling har aktive varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        assertTrue(behandling.forhindrerAutomatisering())
    }

    @Test
    fun `behandling har kun gosysvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        assertTrue(behandling.harKunGosysvarsel())
    }

    @Test
    fun `behandling har ingen varsler og dermed ikke kun gosysvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        assertFalse(behandling.harKunGosysvarsel())
    }

    @Test
    fun `behandling har flere varsler og dermed ikke kun gosysvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId))
        assertFalse(behandling.harKunGosysvarsel())
    }

    @Test
    fun `behandling har ikke aktive varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        assertFalse(behandling.forhindrerAutomatisering())
    }

    @Test
    fun `behandling forhindrer automatisering når den har vurdert - ikke godkjente - varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        behandling.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId, VURDERT),
        )
        behandling.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId, VURDERT),
        )
        assertTrue(behandling.forhindrerAutomatisering())
    }

    @Test
    fun `deaktiverer enkelt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        behandling.håndterNyttVarsel(varsel)
        behandling.håndterDeaktivertVarsel(varsel)
        behandling.assertVarsler(0, VarselStatusDto.AKTIV, SB_EX_1)
        behandling.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)
    }

    @Test
    fun `deaktiverer enkelt varsel basert på varselkode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        behandling.håndterNyttVarsel(varsel)
        behandling.deaktiverVarsel("SB_EX_1")
        behandling.assertVarsler(0, VarselStatusDto.AKTIV, SB_EX_1)
        behandling.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)
    }

    @Test
    fun `sletter varsel om avvik og legger det til på nytt hvis det finnes fra før`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "RV_IV_2", LocalDateTime.now(), vedtaksperiodeId))
        behandling.håndterNyttVarsel(Varsel(varselId, "RV_IV_2", LocalDateTime.now(), vedtaksperiodeId))
        behandling.assertVarsler(1, VarselStatusDto.AKTIV, "RV_IV_2")
    }

    @Test
    fun `Lagrer kun én utgave av et aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))

        behandling.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
    }

    @Test
    fun `kan reaktivere deaktivert varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        behandling.håndterNyttVarsel(varsel)
        behandling.håndterDeaktivertVarsel(varsel)
        behandling.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)

        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))

        behandling.assertVarsler(0, VarselStatusDto.INAKTIV, SB_EX_1)
        behandling.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
    }

    @Test
    fun `kan deaktivere reaktivert varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        behandling.håndterNyttVarsel(varsel)
        behandling.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
        behandling.håndterDeaktivertVarsel(varsel)
        behandling.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)
        behandling.håndterNyttVarsel(varsel)
        behandling.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
        behandling.håndterDeaktivertVarsel(varsel)

        behandling.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)
        behandling.assertVarsler(0, VarselStatusDto.AKTIV, SB_EX_1)
    }

    @Test
    fun `behandling kan motta ny utbetalingId`() {
        val behandling = behandling()
        val utbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(utbetalingId)
    }

    @Test
    fun `behandling kan motta ny utbetalingId så lenge behandlingen ikke er ferdig behandlet`() {
        val behandling = behandling()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(gammelUtbetalingId)
        behandling.håndterForkastetUtbetaling(gammelUtbetalingId)
        behandling.håndterNyUtbetaling(nyUtbetalingId)

        assertEquals(nyUtbetalingId, behandling.toDto().utbetalingId)
    }

    @Test
    fun `Lagrer varsel på behandling selvom den er ferdig behandlet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId, behandlingId = behandlingId)
        behandling.håndterVedtakFattet()
        val varsel = Varsel(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId)
        behandling.håndterNyttVarsel(varsel)
        behandling.assertVarsler(1, VarselStatusDto.AKTIV, "RV_IM_1")
    }

    @Test
    fun `Skal kunne opprette varsel på behandling`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandling(vedtaksperiodeId = vedtaksperiodeId)
        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), SB_EX_1.name, LocalDateTime.now(), vedtaksperiodeId))
        behandling.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
    }

    @Test
    fun `kan ikke knytte utbetalingId til ferdig behandlet behandling som har utbetalingId fra før`() {
        val behandling = behandling()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(gammelUtbetalingId)
        behandling.håndterVedtakFattet()
        behandling.håndterNyUtbetaling(nyUtbetalingId)

        assertEquals(gammelUtbetalingId, behandling.toDto().utbetalingId)
    }

    @Test
    fun `kan ikke knytte utbetalingId til ferdig behandlet behandling som ikke har utbetalingId fra før`() {
        val utbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()

        val behandling = behandling()
        behandling.håndterNyUtbetaling(utbetalingId)

        behandling.håndterVedtakFattet()
        behandling.håndterNyUtbetaling(nyUtbetalingId)
        behandling.assertUtbetalingId(utbetalingId)
    }

    @Test
    fun `kan fjerne utbetalingId fra ubehandlet behandling`() {
        val behandling = behandling()
        val utbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(utbetalingId)
        behandling.assertUtbetalingId(utbetalingId)
        behandling.håndterForkastetUtbetaling(utbetalingId)
        behandling.assertUtbetalingId(null)
    }

    @Test
    fun `kan ikke fjerne utbetalingId fra ferdig behandlet behandling`() {
        val behandling = behandling()
        val utbetalingId = UUID.randomUUID()
        behandling.håndterNyUtbetaling(utbetalingId)
        behandling.håndterVedtakFattet()
        behandling.håndterForkastetUtbetaling(utbetalingId)
        behandling.assertUtbetalingId(utbetalingId)
    }

    @Test
    fun `finn behandling med vedtaksperiodeId`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val behandlingV1 = behandling(vedtaksperiodeId = vedtaksperiodeId1)
        val behandlingV2 = behandling(vedtaksperiodeId = vedtaksperiodeId2)

        assertNotNull(listOf(behandlingV1, behandlingV2).finnBehandlingForVedtaksperiode(vedtaksperiodeId1))
    }

    @Test
    fun `finn behandling med spleisBehandlingId`() {
        val spleisBehandlingId = UUID.randomUUID()
        val behandlingDetSøkesEtter = behandling(spleisBehandlingId = spleisBehandlingId)
        val behandlinger =
            listOf(
                behandlingDetSøkesEtter,
                behandling(spleisBehandlingId = UUID.randomUUID()),
                behandling(spleisBehandlingId = null),
            )
        assertEquals(behandlingDetSøkesEtter, behandlinger.finnBehandlingForSpleisBehandling(spleisBehandlingId))
        assertNull(behandlinger.finnBehandlingForSpleisBehandling(UUID.randomUUID()))
    }

    @Test
    fun `finn siste behandling uten spleisBehandlingId`() {
        val behandlingMedBehandlingIdNull = behandling(spleisBehandlingId = null)
        val behandlinger =
            mutableListOf(
                behandling(spleisBehandlingId = UUID.randomUUID()),
                behandlingMedBehandlingIdNull,
                behandling(spleisBehandlingId = UUID.randomUUID()),
            )
        assertEquals(behandlingMedBehandlingIdNull, behandlinger.finnSisteBehandlingUtenSpleisBehandlingId())

        val nybehandlingMedBehandlingIdNull = behandling(spleisBehandlingId = null)
        behandlinger.add(nybehandlingMedBehandlingIdNull)
        assertEquals(nybehandlingMedBehandlingIdNull, behandlinger.finnSisteBehandlingUtenSpleisBehandlingId())
    }

    @Test
    fun `finner ikke behandling`() {
        val behandlingV1 = behandling()
        assertNull(listOf(behandlingV1).finnBehandlingForVedtaksperiode(UUID.randomUUID()))
    }

    @Test
    fun `har behandling medlemskapsvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandlingMedVarsel(1.februar, 28.februar, vedtaksperiodeId, "RV_MV_1")
        assertTrue(listOf(behandling1).harMedlemskapsvarsel(vedtaksperiodeId))
    }

    @Test
    fun `har minst en behandling medlemskapsvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandlingMedVarsel(1.februar, 28.februar, vedtaksperiodeId, "RV_MV_1")
        val behandling2 = behandling(fom = 1.januar, tom = 31.januar, skjæringstidspunkt = 1.januar)
        assertTrue(listOf(behandling1, behandling2).harMedlemskapsvarsel(vedtaksperiodeId))
    }

    @Test
    fun `har kun åpen oppgave i gosys`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandlingMedVarsel(1.februar, 28.februar, vedtaksperiodeId, "SB_EX_1")
        assertTrue(listOf(behandling).harÅpenGosysOppgave(vedtaksperiodeId))
    }

    @Test
    fun `flere varsler enn kun åpen oppgave i gosys`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling = behandlingMedVarsel(1.februar, 28.februar, vedtaksperiodeId, "SB_EX_1")
        assertTrue(listOf(behandling).harÅpenGosysOppgave(vedtaksperiodeId))

        behandling.håndterNyttVarsel(Varsel(UUID.randomUUID(), "RV_MV_1", LocalDateTime.now(), vedtaksperiodeId))
        assertFalse(listOf(behandling).harÅpenGosysOppgave(vedtaksperiodeId))
    }

    @Test
    fun `behandling mangler medlemskapsvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(vedtaksperiodeId)
        assertFalse(listOf(behandling1).harMedlemskapsvarsel(vedtaksperiodeId))
    }

    @Test
    fun `haster å behandle hvis behandlingen har varsel om negativt beløp`() {
        val behandling1 = behandlingMedVarsel(varselkode = "RV_UT_23")
        assertTrue(behandling1.hasterÅBehandle())
    }

    @Test
    fun `krever skjønnsfastsettelse hvis behandling har varsel om avvik`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = listOf(behandlingMedVarsel(vedtaksperiodeId = vedtaksperiodeId, varselkode = "RV_IV_2"))
        assertTrue(behandling1.kreverSkjønnsfastsettelse(vedtaksperiodeId = vedtaksperiodeId))
    }

    @Test
    fun `haster ikke å behandle hvis behandlingen ikke har varsel om negativt beløp`() {
        val behandling1 = behandling()
        assertFalse(behandling1.hasterÅBehandle())
    }

    @Test
    fun `oppdaterer fom, tom, skjæringstidspunkt, behandlingId`() {
        val behandlingId = UUID.randomUUID()
        val behandling = behandling(fom = 1.januar, tom = 31.januar, skjæringstidspunkt = 1.januar)
        behandling.håndter(mockk(relaxed = true), SpleisVedtaksperiode(UUID.randomUUID(), behandlingId, 2.januar, 30.januar, 2.januar))
        val dto = behandling.toDto()

        assertEquals(2.januar, dto.fom)
        assertEquals(30.januar, dto.tom)
        assertEquals(2.januar, dto.skjæringstidspunkt)
        assertEquals(behandlingId, dto.spleisBehandlingId)
    }

    @Test
    fun `referential equals`() {
        val behandling = behandling(UUID.randomUUID(), UUID.randomUUID())
        assertEquals(behandling, behandling)
        assertEquals(behandling.hashCode(), behandling.hashCode())
    }

    @Test
    fun `structural equals`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId)
        val utbetalingId = UUID.randomUUID()
        behandling1.håndterNyUtbetaling(utbetalingId)
        behandling1.oppdaterBehandlingsinformasjon(emptyList(), spleisBehandlingId, utbetalingId)
        behandling1.håndterVedtakFattet()
        val behandling2 = behandling(behandlingId, vedtaksperiodeId)
        behandling2.håndterNyUtbetaling(utbetalingId)
        behandling2.oppdaterBehandlingsinformasjon(emptyList(), spleisBehandlingId, utbetalingId)
        behandling2.håndterVedtakFattet()
        assertEquals(behandling1, behandling2)
        assertEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig behandlingIder`() {
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId1, vedtaksperiodeId)
        val behandling2 = behandling(behandlingId2, vedtaksperiodeId)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellige vedtaksperiodeIder`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId1)
        val behandling2 = behandling(behandlingId, vedtaksperiodeId2)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig spleisBehandlingId og tags`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId)
        val spleisBehandlingId = UUID.randomUUID()
        val utbetalingId1 = UUID.randomUUID()
        behandling1.håndterNyUtbetaling(utbetalingId1)
        behandling1.oppdaterBehandlingsinformasjon(listOf("hei"), spleisBehandlingId, utbetalingId1)
        val behandling2 = behandling(behandlingId, vedtaksperiodeId)
        val spleisBehandlingId2 = UUID.randomUUID()
        val utbetalingId2 = UUID.randomUUID()
        behandling2.håndterNyUtbetaling(utbetalingId2)
        behandling2.oppdaterBehandlingsinformasjon(listOf("hallo"), spleisBehandlingId2, utbetalingId2)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig utbetalingId`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId)
        behandling1.håndterNyUtbetaling(UUID.randomUUID())
        val behandling2 = behandling(behandlingId, vedtaksperiodeId)
        behandling2.håndterNyUtbetaling(UUID.randomUUID())
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig utbetalingId der én behandling har null`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId)
        behandling1.håndterNyUtbetaling(UUID.randomUUID())
        val behandling2 = behandling(behandlingId, vedtaksperiodeId)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig periode`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId, fom = 1.januar, tom = 31.januar)
        val behandling2 = behandling(behandlingId, vedtaksperiodeId, fom = 1.februar, tom = 28.februar)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig skjæringstidspunkt`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId, skjæringstidspunkt = 1.januar)
        val behandling2 = behandling(behandlingId, vedtaksperiodeId, skjæringstidspunkt = 1.februar)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `forskjellig tilstand`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val behandling1 = behandling(behandlingId, vedtaksperiodeId, skjæringstidspunkt = 1.januar)
        behandling1.avsluttetUtenVedtak()
        val behandling2 = behandling(behandlingId, vedtaksperiodeId, skjæringstidspunkt = 1.januar)
        assertNotEquals(behandling1, behandling2)
        assertNotEquals(behandling1.hashCode(), behandling2.hashCode())
    }

    @Test
    fun `behandling toDto`() {
        val behandlingId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val fom = 1.januar
        val tom = 31.januar
        val skjæringstidspunkt = 1.januar
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val behandling = Behandling(behandlingId, vedtaksperiodeId, fom, tom, skjæringstidspunkt)
        behandling.håndterNyUtbetaling(utbetalingId)
        val tags = listOf("tag 1")
        behandling.oppdaterBehandlingsinformasjon(tags, spleisBehandlingId, utbetalingId)

        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val varsel = Varsel(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, AKTIV)
        behandling.håndterNyttVarsel(varsel)
        val dto = behandling.toDto()

        assertEquals(
            BehandlingDto(
                behandlingId,
                vedtaksperiodeId,
                utbetalingId,
                spleisBehandlingId,
                skjæringstidspunkt,
                fom,
                tom,
                TilstandDto.KlarTilBehandling,
                tags,
                null,
                listOf(varsel.toDto()),
            ),
            dto,
        )
    }

    @Test
    fun `behandlingTilstand toDto`() {
        assertEquals(TilstandDto.VedtakFattet, Behandling.VedtakFattet.toDto())
        assertEquals(TilstandDto.VidereBehandlingAvklares, Behandling.VidereBehandlingAvklares.toDto())
        assertEquals(TilstandDto.AvsluttetUtenVedtak, Behandling.AvsluttetUtenVedtak.toDto())
        assertEquals(TilstandDto.AvsluttetUtenVedtakMedVarsler, Behandling.AvsluttetUtenVedtakMedVarsler.toDto())
    }

    private fun behandlingMedVarsel(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        varselkode: String = "SB_EX_1",
    ): Behandling =
        behandling(vedtaksperiodeId = vedtaksperiodeId, fom = fom, tom = tom).also {
            it.håndterNyttVarsel(Varsel(UUID.randomUUID(), varselkode, LocalDateTime.now(), vedtaksperiodeId))
        }

    private fun behandling(
        behandlingId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        spleisBehandlingId: UUID? = null,
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = 1.januar,
    ) = Behandling(
        id = behandlingId,
        vedtaksperiodeId = vedtaksperiodeId,
        spleisBehandlingId = spleisBehandlingId,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
    )

    private fun Behandling.assertVarsler(
        forventetAntall: Int,
        status: VarselStatusDto,
        varselkode: Varselkode,
    ) {
        this.assertVarsler(forventetAntall, status, varselkode.name)
    }

    private fun Behandling.assertVarsler(
        forventetAntall: Int,
        status: VarselStatusDto,
        varselkode: String,
    ) {
        val dto = this.toDto()
        val varsler = dto.varsler
        val varsel = varsler.filter { it.varselkode == varselkode && it.status == status }
        assertEquals(forventetAntall, varsel.size)
    }

    private fun Behandling.assertUtbetalingId(utbetalingId: UUID?) {
        val dto = this.toDto()
        assertEquals(utbetalingId, dto.utbetalingId)
    }
}
