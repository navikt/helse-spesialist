package no.nav.helse.modell.vedtaksperiode

import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.AKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.INAKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.VURDERT
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.varsel.VarselRepository
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.vedtak.AvsluttetUtenVedtak
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnGenerasjonForSpleisBehandling
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnGenerasjonForVedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnSisteGenerasjonUtenSpleisBehandlingId
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.kreverSkjønnsfastsettelse
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.kreverTotrinnsvurdering
import no.nav.helse.modell.vedtaksperiode.Periode.Companion.til
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class GenerasjonTest : AbstractDatabaseTest() {
    private val varselRepository = VarselRepository(dataSource)
    private val generasjonRepository = GenerasjonRepository(dataSource)
    private val generasjonDao = GenerasjonDao(dataSource)
    private lateinit var generasjonId: UUID
    private lateinit var observer: GenerasjonTestObserver

    @BeforeEach
    internal fun beforeEach() {
        lagVarseldefinisjoner()
        observer = GenerasjonTestObserver()
    }

    @Test
    fun `generasjon ligger før dato`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), 1.januar, 31.januar, 1.januar)
        assertTrue(generasjon.tilhører(31.januar))
        assertTrue(generasjon.tilhører(1.februar))
        assertFalse(generasjon.tilhører(1.januar))
        assertFalse(generasjon.tilhører(31.desember(2017)))
    }

    @Test
    fun `generasjon har aktive varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        assertTrue(generasjon.forhindrerAutomatisering())
    }

    @Test
    fun `generasjon har ikke aktive varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        assertFalse(generasjon.forhindrerAutomatisering())
    }

    @Test
    fun `generasjon forhindrer automatisering når den har vurdert - ikke godkjente - varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId, VURDERT),
            UUID.randomUUID(),
        )
        generasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId, VURDERT),
            UUID.randomUUID(),
        )
        assertTrue(generasjon.forhindrerAutomatisering())
    }

    @Test
    fun `deaktiverer enkelt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndterNyttVarsel(varsel, UUID.randomUUID())
        generasjon.håndterDeaktivertVarsel(varsel)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)
    }

    @Test
    fun `deaktiverer enkelt varsel basert på varselkode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndterNyttVarsel(varsel, UUID.randomUUID())
        generasjon.deaktiverVarsel("SB_EX_1")
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)
    }

    @Test
    fun `sletter varsel om avvik og legger det til på nytt hvis det finnes fra før`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "RV_IV_2", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        generasjon.håndterNyttVarsel(Varsel(varselId, "RV_IV_2", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        val varsler = generasjon.toDto().varsler
        assertEquals(1, varsler.size)
        val varsel = varsler.single()
        assertEquals(VarselStatusDto.AKTIV, varsel.status)
        assertEquals("RV_IV_2", varsel.varselkode)
    }

    @Test
    fun `Lagrer kun én utgave av et aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())

        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `kan reaktivere deaktivert varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndterNyttVarsel(varsel, UUID.randomUUID())
        generasjon.håndterDeaktivertVarsel(varsel)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)

        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())

        assertVarsler(generasjonId, 0, INAKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `kan deaktivere reaktivert varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndterNyttVarsel(varsel, UUID.randomUUID())
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
        generasjon.håndterDeaktivertVarsel(varsel)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)
        generasjon.håndterNyttVarsel(varsel, UUID.randomUUID())
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
        generasjon.håndterDeaktivertVarsel(varsel)

        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
    }

    @Test
    fun `Generasjon kan motta ny utbetalingId`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
    }

    @Test
    fun `Generasjon kan motta ny utbetalingId så lenge generasjonen ikke er ferdig behandlet`() {
        val generasjon = nyGenerasjon()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), gammelUtbetalingId)
        generasjon.håndterForkastetUtbetaling(gammelUtbetalingId)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId)

        assertEquals(nyUtbetalingId, generasjon.toDto().utbetalingId)
    }

    @Test
    fun `Lagrer varsel på generasjon selvom den er ferdig behandlet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, id = generasjonId)
        generasjon.registrer(observer)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val varsel = Varsel(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndterNyttVarsel(varsel, UUID.randomUUID())

        assertEquals(1, observer.opprettedeVarsler[generasjonId]?.size)
        assertEquals("RV_IM_1", observer.opprettedeVarsler[generasjonId]?.get(0))
        assertAntallGenerasjoner(1, vedtaksperiodeId)
    }

    @Test
    fun `Skal kunne opprette varsel på generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), SB_EX_1.name, LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        assertAntallGenerasjoner(1, vedtaksperiodeId)
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `kan ikke knytte utbetalingId til ferdig behandlet generasjon som har utbetalingId fra før`() {
        val generasjon = nyGenerasjon()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), gammelUtbetalingId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId)

        assertEquals(gammelUtbetalingId, generasjon.toDto().utbetalingId)
    }

    @Test
    fun `kan ikke knytte utbetalingId til ferdig behandlet generasjon som ikke har utbetalingId fra før`() {
        val generasjon = nyGenerasjon()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId)
        assertIkkeUtbetaling(generasjonId, nyUtbetalingId)
    }

    @Test
    fun `kan fjerne utbetalingId fra ubehandlet generasjon`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        assertEquals(utbetalingId, generasjon.toDto().utbetalingId)
        generasjon.håndterForkastetUtbetaling(utbetalingId)
        assertEquals(null, generasjon.toDto().utbetalingId)
    }

    @Test
    fun `kan ikke fjerne utbetalingId fra ferdig behandlet generasjon`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterForkastetUtbetaling(utbetalingId)
        assertEquals(utbetalingId, generasjon.toDto().utbetalingId)
    }

    @Test
    fun `finn generasjon med vedtaksperiodeId`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonV1 = generasjon(vedtaksperiodeId = vedtaksperiodeId1)
        val generasjonV2 = generasjon(vedtaksperiodeId = vedtaksperiodeId2)

        assertNotNull(listOf(generasjonV1, generasjonV2).finnGenerasjonForVedtaksperiode(vedtaksperiodeId1))
    }

    @Test
    fun `finn generasjon med spleisBehandlingId`() {
        val spleisBehandlingId = UUID.randomUUID()
        val generasjonDetSøkesEtter = generasjon(spleisBehandlingId = spleisBehandlingId)
        val generasjoner =
            listOf(
                generasjonDetSøkesEtter,
                generasjon(spleisBehandlingId = UUID.randomUUID()),
                generasjon(spleisBehandlingId = null),
            )
        assertEquals(generasjonDetSøkesEtter, generasjoner.finnGenerasjonForSpleisBehandling(spleisBehandlingId))
        assertNull(generasjoner.finnGenerasjonForSpleisBehandling(UUID.randomUUID()))
    }

    @Test
    fun `finn siste generasjon uten spleisBehandlingId`() {
        val generasjonMedBehandlingIdNull = generasjon(spleisBehandlingId = null)
        val generasjoner =
            mutableListOf(
                generasjon(spleisBehandlingId = UUID.randomUUID()),
                generasjonMedBehandlingIdNull,
                generasjon(spleisBehandlingId = UUID.randomUUID()),
            )
        assertEquals(generasjonMedBehandlingIdNull, generasjoner.finnSisteGenerasjonUtenSpleisBehandlingId())

        val nyGenerasjonMedBehandlingIdNull = generasjon(spleisBehandlingId = null)
        generasjoner.add(nyGenerasjonMedBehandlingIdNull)
        assertEquals(nyGenerasjonMedBehandlingIdNull, generasjoner.finnSisteGenerasjonUtenSpleisBehandlingId())
    }

    @Test
    fun `finner ikke generasjon`() {
        val generasjonV1 = generasjon()
        assertNull(listOf(generasjonV1).finnGenerasjonForVedtaksperiode(UUID.randomUUID()))
    }

    @Test
    fun `krever totrinnsvurdering hvis generasjonen har medlemskapsvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(1.februar, 28.februar, vedtaksperiodeId, "RV_MV_1")
        assertTrue(listOf(generasjon1).kreverTotrinnsvurdering(vedtaksperiodeId))
    }

    @Test
    fun `krever totrinnsvurdering hvis en generasjon av flere har medlemskapsvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(1.februar, 28.februar, vedtaksperiodeId, "RV_MV_1")
        val generasjon2 = generasjon(fom = 1.januar, tom = 31.januar, skjæringstidspunkt = 1.januar)
        assertTrue(listOf(generasjon1, generasjon2).kreverTotrinnsvurdering(vedtaksperiodeId))
    }

    @Test
    fun `krever ikke totrinnsvurdering hvis generasjonen ikke har medlemskapsvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(vedtaksperiodeId)
        assertFalse(listOf(generasjon1).kreverTotrinnsvurdering(vedtaksperiodeId))
    }

    @Test
    fun `haster å behandle hvis generasjonen har varsel om negativt beløp`() {
        val generasjon1 = generasjonMedVarsel(varselkode = "RV_UT_23")
        assertTrue(generasjon1.hasterÅBehandle())
    }

    @Test
    fun `krever skjønnsfastsettelse hvis generasjon har varsel om avvik`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = listOf(generasjonMedVarsel(vedtaksperiodeId = vedtaksperiodeId, varselkode = "RV_IV_2"))
        assertTrue(generasjon1.kreverSkjønnsfastsettelse(vedtaksperiodeId = vedtaksperiodeId))
    }

    @Test
    fun `haster ikke å behandle hvis generasjonen ikke har varsel om negativt beløp`() {
        val generasjon1 = generasjon()
        assertFalse(generasjon1.hasterÅBehandle())
    }

    @Test
    fun `oppdaterer fom, tom, skjæringstidspunkt, behandlingId`() {
        val behandlingId = UUID.randomUUID()
        val generasjon = generasjon(fom = 1.januar, tom = 31.januar, skjæringstidspunkt = 1.januar)
        generasjon.håndter(mockk(relaxed = true), SpleisVedtaksperiode(UUID.randomUUID(), behandlingId, 2.januar, 30.januar, 2.januar))
        val dto = generasjon.toDto()

        assertEquals(2.januar, dto.fom)
        assertEquals(30.januar, dto.tom)
        assertEquals(2.januar, dto.skjæringstidspunkt)
        assertEquals(behandlingId, dto.spleisBehandlingId)
    }

    @Test
    fun `kan motta avsluttet_uten_vedtak i IngenUtbetalingMåVurderes`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjonMedVarsel(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        assertEquals(TilstandDto.AvsluttetUtenVedtakMedVarsler, generasjon.toDto().tilstand)
        assertDoesNotThrow {
            generasjon.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        }
        assertEquals(TilstandDto.AvsluttetUtenVedtakMedVarsler, generasjon.toDto().tilstand)
    }

    @Test
    fun `referential equals`() {
        val generasjon = generasjon(UUID.randomUUID(), UUID.randomUUID())
        assertEquals(generasjon, generasjon)
        assertEquals(generasjon.hashCode(), generasjon.hashCode())
    }

    @Test
    fun `structural equals`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId)
        val utbetalingId = UUID.randomUUID()
        generasjon1.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        generasjon1.oppdaterBehandlingsinformasjon(emptyList(), spleisBehandlingId, utbetalingId)
        generasjon1.håndterVedtakFattet(UUID.randomUUID())
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId)
        generasjon2.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        generasjon2.oppdaterBehandlingsinformasjon(emptyList(), spleisBehandlingId, utbetalingId)
        generasjon2.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(generasjon1, generasjon2)
        assertEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig generasjonIder`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId1, vedtaksperiodeId)
        val generasjon2 = generasjon(generasjonId2, vedtaksperiodeId)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellige vedtaksperiodeIder`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId1)
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId2)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig spleisBehandlingId og tags`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId)
        val spleisBehandlingId = UUID.randomUUID()
        val utbetalingId1 = UUID.randomUUID()
        generasjon1.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId1)
        generasjon1.oppdaterBehandlingsinformasjon(listOf("hei"), spleisBehandlingId, utbetalingId1)
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId)
        val spleisBehandlingId2 = UUID.randomUUID()
        val utbetalingId2 = UUID.randomUUID()
        generasjon2.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId2)
        generasjon2.oppdaterBehandlingsinformasjon(listOf("hallo"), spleisBehandlingId2, utbetalingId2)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig utbetalingId`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId)
        generasjon1.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID())
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId)
        generasjon2.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID())
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig utbetalingId der én generasjon har null`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId)
        generasjon1.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID())
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig periode`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId, fom = 1.januar, tom = 31.januar)
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId, fom = 1.februar, tom = 28.februar)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig skjæringstidspunkt`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId, skjæringstidspunkt = 1.januar)
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId, skjæringstidspunkt = 1.februar)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig tilstand`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId, skjæringstidspunkt = 1.januar)
        generasjon1.avsluttetUtenVedtak(AvsluttetUtenVedtak(vedtaksperiodeId, emptyList(), UUID.randomUUID()), SykepengevedtakBuilder())
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId, skjæringstidspunkt = 1.januar)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `generasjon toDto`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val fom = 1.januar
        val tom = 31.januar
        val skjæringstidspunkt = 1.januar
        val utbetalingId = UUID.randomUUID()
        val spleisBehandlingId = UUID.randomUUID()
        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, fom, tom, skjæringstidspunkt)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        val tags = listOf("tag 1")
        generasjon.oppdaterBehandlingsinformasjon(tags, spleisBehandlingId, utbetalingId)

        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val varsel = Varsel(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, AKTIV)
        generasjon.håndterNyttVarsel(varsel, UUID.randomUUID())
        val dto = generasjon.toDto()

        assertEquals(
            GenerasjonDto(
                generasjonId,
                vedtaksperiodeId,
                utbetalingId,
                spleisBehandlingId,
                skjæringstidspunkt,
                fom,
                tom,
                TilstandDto.KlarTilBehandling,
                tags,
                listOf(varsel.toDto()),
            ),
            dto,
        )
    }

    @Test
    fun `generasjonTilstand toDto`() {
        assertEquals(TilstandDto.VedtakFattet, Generasjon.VedtakFattet.toDto())
        assertEquals(TilstandDto.VidereBehandlingAvklares, Generasjon.VidereBehandlingAvklares.toDto())
        assertEquals(TilstandDto.AvsluttetUtenVedtak, Generasjon.AvsluttetUtenVedtak.toDto())
        assertEquals(TilstandDto.AvsluttetUtenVedtakMedVarsler, Generasjon.AvsluttetUtenVedtakMedVarsler.toDto())
    }

    private fun generasjonMedVarsel(
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        varselkode: String = "SB_EX_1",
    ): Generasjon {
        return generasjon(vedtaksperiodeId = vedtaksperiodeId, fom = fom, tom = tom).also {
            it.håndterNyttVarsel(Varsel(UUID.randomUUID(), varselkode, LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
            it.registrer(observer)
        }
    }

    private fun nyGenerasjon(
        id: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
    ): Generasjon {
        generasjonId = id
        val generasjon =
            generasjonDao.opprettFor(
                generasjonId,
                vedtaksperiodeId,
                UUID.randomUUID(),
                1.januar,
                1.januar til 31.januar,
                Generasjon.VidereBehandlingAvklares,
            )
        generasjon.registrer(generasjonRepository)
        return generasjon
    }

    private fun generasjon(
        generasjonId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        spleisBehandlingId: UUID? = null,
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = 1.januar,
    ) = Generasjon(
        id = generasjonId,
        vedtaksperiodeId = vedtaksperiodeId,
        spleisBehandlingId = spleisBehandlingId,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt,
    ).also {
        it.registrer(observer)
    }

    private fun assertVarsler(
        generasjonId: UUID,
        forventetAntall: Int,
        status: Status,
        varselkode: String,
        varselId: UUID? = null,
    ) {
        @Language("PostgreSQL")
        val query =
            """SELECT COUNT(1) FROM selve_varsel sv INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
               WHERE svg.unik_id = :generasjonId 
               AND sv.status = :status 
               AND sv.kode = :varselkode
               AND 
                 CASE 
                   WHEN :skalSjekkeVarselId = true THEN sv.unik_id = :varselId
                   ELSE true
                 END
            """
        val antall =
            sessionOf(dataSource).use { session ->
                session.run(
                    queryOf(
                        query,
                        mapOf(
                            "generasjonId" to generasjonId,
                            "status" to status.name,
                            "varselkode" to varselkode,
                            "skalSjekkeVarselId" to (varselId != null),
                            "varselId" to varselId,
                        ),
                    ).map { it.int(1) }.asSingle,
                )
            }
        assertEquals(forventetAntall, antall)
    }

    private fun assertVarsler(
        generasjonId: UUID,
        forventetAntall: Int,
        status: Status,
        varselkode: Varselkode,
        varselId: UUID? = null,
    ) {
        assertVarsler(generasjonId, forventetAntall, status, varselkode.name, varselId)
    }

    private fun assertIkkeUtbetaling(
        generasjonId: UUID,
        utbetalingId: UUID,
    ) {
        @Language("PostgreSQL")
        val query =
            """SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = ? AND utbetaling_id = ?
            """
        val antall =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, generasjonId, utbetalingId).map { it.int(1) }.asSingle)
            }
        assertEquals(0, antall)
    }

    private fun assertAntallGenerasjoner(
        forventetAntall: Int,
        vedtaksperiodeId: UUID,
    ) {
        @Language("PostgreSQL")
        val query =
            """SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon svg WHERE svg.vedtaksperiode_id = ?
            """
        val antall =
            sessionOf(dataSource).use { session ->
                session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
            }
        assertEquals(forventetAntall, antall)
    }

    private fun lagVarseldefinisjoner() {
        Varselkode.entries.forEach { varselkode ->
            lagVarseldefinisjon(varselkode.name)
        }
    }

    private fun lagVarseldefinisjon(varselkode: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO api_varseldefinisjon(unik_id, kode, tittel, forklaring, handling, avviklet, opprettet) VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (unik_id) DO NOTHING"
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    query,
                    UUID.nameUUIDFromBytes(varselkode.toByteArray()),
                    varselkode,
                    "En tittel for varselkode=$varselkode",
                    "En forklaring for varselkode=$varselkode",
                    "En handling for varselkode=$varselkode",
                    false,
                    LocalDateTime.now(),
                ).asUpdate,
            )
        }
    }
}
