package no.nav.helse.modell.vedtaksperiode

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.helse.AbstractDatabaseTest
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
import no.nav.helse.modell.varsel.Varsel.Companion.lagre
import no.nav.helse.modell.varsel.Varsel.Status
import no.nav.helse.modell.varsel.Varsel.Status.AKTIV
import no.nav.helse.modell.varsel.Varsel.Status.AVVIST
import no.nav.helse.modell.varsel.Varsel.Status.GODKJENT
import no.nav.helse.modell.varsel.Varsel.Status.INAKTIV
import no.nav.helse.modell.varsel.Varsel.Status.VURDERT
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.varsel.Varselkode.SB_EX_2
import no.nav.helse.modell.varsel.Varselkode.SB_EX_3
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class GenerasjonTest: AbstractDatabaseTest() {
    private val varselRepository = ActualVarselRepository(dataSource)
    private val generasjonRepository = ActualGenerasjonRepository(dataSource)
    private lateinit var generasjonId: UUID
    private lateinit var observer: Observer

    @BeforeEach
    internal fun beforeEach() {
        lagVarseldefinisjoner()
        observer = Observer
    }

    @Test
    fun `Kan registrere observer`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = Generasjon(generasjonId, UUID.randomUUID(), generasjonRepository)
        val observer = object : IVedtaksperiodeObserver {
            lateinit var generasjonId: UUID
            lateinit var fom: LocalDate
            lateinit var tom: LocalDate
            lateinit var skjæringstidspunkt: LocalDate
            override fun tidslinjeOppdatert(
                generasjonId: UUID,
                fom: LocalDate,
                tom: LocalDate,
                skjæringstidspunkt: LocalDate
            ) {
                this.generasjonId = generasjonId
                this.fom = fom
                this.tom = tom
                this.skjæringstidspunkt = skjæringstidspunkt
            }
        }
        generasjon.registrer(observer)
        generasjon.håndterTidslinjeendring(1.januar, 31.januar, 1.januar)
        assertEquals(generasjonId, observer.generasjonId)
        assertEquals(1.januar, observer.fom)
        assertEquals(31.januar, observer.tom)
        assertEquals(1.januar, observer.skjæringstidspunkt)
    }

    @Test
    fun `Generasjon er ikke å anse som oppdatert dersom fom, tom og skjæringstidspunkt er lik oppdatering`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = Generasjon(generasjonId, UUID.randomUUID(), null, false, 1.januar, Periode(1.januar, 31.januar), emptySet(), dataSource)
        val observer = object : IVedtaksperiodeObserver {
            var generasjonId: UUID? = null
            var fom: LocalDate? = null
            var tom: LocalDate? = null
            var skjæringstidspunkt: LocalDate? = null
            override fun tidslinjeOppdatert(
                generasjonId: UUID,
                fom: LocalDate,
                tom: LocalDate,
                skjæringstidspunkt: LocalDate
            ) {
                this.generasjonId = generasjonId
                this.fom = fom
                this.tom = tom
                this.skjæringstidspunkt = skjæringstidspunkt
            }
        }
        generasjon.registrer(observer)
        generasjon.håndterTidslinjeendring(1.januar, 31.januar, 1.januar)
        assertNull(observer.generasjonId)
        assertNull(observer.fom)
        assertNull(observer.tom)
        assertNull(observer.skjæringstidspunkt)
    }

    @Test
    fun `Generasjon skal ikke oppdateres dersom den er låst`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = Generasjon(generasjonId, UUID.randomUUID(), null, låst = true, null, null, emptySet(), dataSource)
        val observer = object : IVedtaksperiodeObserver {
            var generasjonId: UUID? = null
            var fom: LocalDate? = null
            var tom: LocalDate? = null
            var skjæringstidspunkt: LocalDate? = null
            override fun tidslinjeOppdatert(
                generasjonId: UUID,
                fom: LocalDate,
                tom: LocalDate,
                skjæringstidspunkt: LocalDate
            ) {
                this.generasjonId = generasjonId
                this.fom = fom
                this.tom = tom
                this.skjæringstidspunkt = skjæringstidspunkt
            }
        }
        generasjon.registrer(observer)
        generasjon.håndterTidslinjeendring(1.januar, 31.januar, 1.januar)
        assertNull(observer.generasjonId)
        assertNull(observer.fom)
        assertNull(observer.tom)
        assertNull(observer.skjæringstidspunkt)
    }

    @Test
    fun `håndterTidslinjeendring setter fom, tom og skjæringstidspunkt på generasjonen`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val generasjon = nyGenerasjon(id = generasjonId, vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterTidslinjeendring(1.januar, 31.januar, 1.januar)
        assertEquals(
            Generasjon(generasjonId, vedtaksperiodeId, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet(), dataSource),
            generasjon
        )
    }
    @Test
    fun `håndterTidslinjeendring setter ikke fom, tom og skjæringstidspunkt på generasjonen hvis den er låst`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, null, true, null, null, emptySet(), dataSource)
        generasjon.håndterTidslinjeendring(1.januar, 31.januar, 1.januar)
        assertEquals(
            Generasjon(generasjonId, vedtaksperiodeId, null, true, null, null, emptySet(), dataSource),
            generasjon
        )
    }

    @Test
    fun `generasjon ligger før dato`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), null, true, 1.januar, Periode(1.januar, 31.januar), emptySet(), dataSource)
        assertTrue(generasjon.liggerFør(31.januar))
        assertTrue(generasjon.liggerFør(1.februar))
        assertFalse(generasjon.liggerFør(1.januar))
        assertFalse(generasjon.liggerFør(31.desember(2017)))
    }

    @Test
    fun `generasjon ligger ikke før dato dersom perioden er null`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), null, true, null, null, emptySet(), dataSource)
        assertFalse(generasjon.liggerFør(31.januar))
        assertFalse(generasjon.liggerFør(1.februar))
        assertFalse(generasjon.liggerFør(1.januar))
        assertFalse(generasjon.liggerFør(31.desember(2017)))
    }

    @Test
    fun `generasjon har aktive varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val aktivtVarsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId, AKTIV)
        val generasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, null, true, null, null, setOf(aktivtVarsel), dataSource)
        assertTrue(generasjon.harAktiveVarsler())
    }

    @Test
    fun `generasjon har ikke aktive varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, null, true, null, null, emptySet(), dataSource)
        assertFalse(generasjon.harAktiveVarsler())
    }

    @Test
    fun `generasjon har aktive varsler når generasjon har både aktive og vurderte varsler`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val aktivtVarsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId, AKTIV)
        val vurdertVarsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId, VURDERT)
        val generasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, null, true, null, null, setOf(aktivtVarsel, vurdertVarsel), dataSource)
        assertTrue(generasjon.harAktiveVarsler())
    }

    @Test
    fun `opprett neste`() {
        val nyGenerasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet(), dataSource)
        val hendelseId = UUID.randomUUID()
        generasjon.registrer(observer)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nyGenerasjon = generasjon.håndterNyGenerasjon(varselRepository, hendelseId, nyGenerasjonId)
        assertEquals(1, observer.opprettedeGenerasjoner.size)
        assertNotEquals(generasjon, nyGenerasjon)
        observer.assertOpprettelse(nyGenerasjonId, vedtaksperiodeId, hendelseId, 1.januar, 31.januar, 1.januar)
        assertEquals(
            Generasjon(nyGenerasjonId, vedtaksperiodeId, null, false, 1.januar, Periode(1.januar, 31.januar), emptySet(), dataSource),
            nyGenerasjon
        )
    }

    @Test
    fun `ikke opprett neste dersom nåværende er ulåst`() {
        val nyGenerasjonId = UUID.randomUUID()
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), generasjonRepository)
        generasjon.registrer(observer)
        val nyGenerasjon = generasjon.håndterNyGenerasjon(varselRepository, UUID.randomUUID(), nyGenerasjonId)
        assertEquals(0, observer.opprettedeGenerasjoner.size)
        assertNull(nyGenerasjon)
    }

    @Test
    fun `Kopierer skjæringstidspunkt og periode til neste generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        val periode = Periode(1.januar, 5.januar)
        generasjon.håndterTidslinjeendring(skjæringstidspunkt = 1.januar, fom = 1.januar, tom = 5.januar)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nyGenerasjonId = UUID.randomUUID()
        val nyGenerasjon = generasjon.håndterNyGenerasjon(varselRepository, UUID.randomUUID(), nyGenerasjonId)
        val forventetGenerasjon = Generasjon(nyGenerasjonId, vedtaksperiodeId, null, false, 1.januar, periode, emptySet(), dataSource)

        assertEquals(forventetGenerasjon, nyGenerasjon)
    }

    @Test
    fun `flytter aktive varsler til neste generasjon når den opprettes`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nyGenerasjonId = UUID.randomUUID()
        generasjon.håndterNyGenerasjon(varselRepository, UUID.randomUUID(), nyGenerasjonId)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(nyGenerasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `flytter ikke varsler som har en annen status enn aktiv`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterDeaktivertVarsel("SB_EX_1", varselRepository)
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), varselRepository)
        generasjon.håndterGodkjentVarsel("SB_EX_2", "EN_IDENT", varselRepository)
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_3", LocalDateTime.now(), varselRepository)
        generasjon.håndterAvvistAvSaksbehandler("EN_IDENT", varselRepository)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nyGenerasjonId = UUID.randomUUID()
        generasjon.håndterNyGenerasjon(varselRepository, UUID.randomUUID(), nyGenerasjonId)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, GODKJENT, SB_EX_2)
        assertVarsler(generasjonId, 1, AVVIST, SB_EX_3)
        assertVarsler(nyGenerasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(nyGenerasjonId, 0, INAKTIV, SB_EX_1)
        assertVarsler(nyGenerasjonId, 0, GODKJENT, SB_EX_2)
        assertVarsler(nyGenerasjonId, 0, AVVIST, SB_EX_3)
    }

    @Test
    fun `godkjenner enkelt varsel`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterGodkjentVarsel("SB_EX_1", "EN_IDENT", varselRepository)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, GODKJENT, SB_EX_1)
    }

    @Test
    fun `deaktiverer enkelt varsel`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterDeaktivertVarsel("SB_EX_1", varselRepository)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)
    }
    @Test
    fun `godkjenner alle varsler når generasjonen blir godkjent`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID(), varselRepository)
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), varselRepository)
        generasjon.håndterGodkjentAvSaksbehandler("EN_IDENT", varselRepository)
        assertVarsler(generasjonId, 1, GODKJENT, SB_EX_1)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, GODKJENT, SB_EX_2)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_2)
    }

    @Test
    fun `avviser alle varsler når generasjonen blir avvist`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), varselRepository)
        generasjon.håndterAvvistAvSaksbehandler("EN_IDENT", varselRepository)
        assertVarsler(generasjonId, 1, AVVIST, SB_EX_1)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, AVVIST, SB_EX_2)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_2)
    }

    @Test
    fun `Lagrer kun én utgave av et aktivt varsel`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)

        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `kan reaktivere deaktivert varsel`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjon.håndterDeaktivertVarsel("SB_EX_1", varselRepository)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)

        generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)

        assertVarsler(generasjonId, 0, INAKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `kan deaktivere reaktivert varsel`() {
        val generasjon = nyGenerasjon()
        generasjon.håndterSaksbehandlingsvarsel(UUID.randomUUID(), SB_EX_1, LocalDateTime.now(), varselRepository)
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
        generasjon.håndterDeaktivertVarsel("SB_EX_1", varselRepository)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)
        generasjon.håndterSaksbehandlingsvarsel(UUID.randomUUID(), SB_EX_1, LocalDateTime.now(), varselRepository)
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
        generasjon.håndterDeaktivertVarsel("SB_EX_1", varselRepository)

        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
    }

    @Test
    fun `Generasjon kan motta ny utbetalingId`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
    }

    @Test
    fun `Generasjon kan motta ny utbetalingId så lenge generasjonen ikke er låst`() {
        val generasjon = nyGenerasjon()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), gammelUtbetalingId, varselRepository)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId, varselRepository)

        assertIkkeUtbetaling(generasjonId, gammelUtbetalingId)
        assertUtbetaling(generasjonId, nyUtbetalingId)
    }

    @Test
    fun `Oppretter ny generasjon ved varsel på låst generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val utbetalingId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())

        assertAntallGenerasjoner(1, vedtaksperiodeId)
        val nyGenerasjon = generasjon.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        nyGenerasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)

        assertNotEquals(generasjon, nyGenerasjon)
        assertAntallGenerasjoner(2, vedtaksperiodeId)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(utbetalingId, vedtaksperiodeId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `Oppretter ikke ny generasjon når nye varsler som kommer inn samtidig på en låst generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val varsler = listOf(
            Varsel(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId),
            Varsel(UUID.randomUUID(), "RV_IM_2", LocalDateTime.now(), vedtaksperiodeId),
        )
        varsler.lagre(UUID.randomUUID(), varselRepository, generasjonRepository)

        assertAntallGenerasjoner(2, vedtaksperiodeId)
    }

    @Test
    fun `Skal ikke kunne opprette saksbehandlingsvarsel på låst generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())

        assertThrows<IllegalStateException> {
            generasjon.håndterSaksbehandlingsvarsel(UUID.randomUUID(), SB_EX_1, LocalDateTime.now(), varselRepository)
        }
        assertAntallGenerasjoner(1, vedtaksperiodeId)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
    }
    @Test
    fun `Skal kunne opprette saksbehandlingsvarsel på ulåst generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)

        generasjon.håndterSaksbehandlingsvarsel(UUID.randomUUID(), SB_EX_1, LocalDateTime.now(), varselRepository)
        assertAntallGenerasjoner(1, vedtaksperiodeId)
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som har utbetalingId fra før`() {
        val generasjon = nyGenerasjon()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), gammelUtbetalingId, varselRepository)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId, varselRepository)

        assertUtbetaling(generasjonId, gammelUtbetalingId)
        assertIkkeUtbetaling(generasjonId, nyUtbetalingId)
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som ikke har utbetalingId fra før`() {
        val generasjon = nyGenerasjon()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId, varselRepository)
        assertIkkeUtbetaling(generasjonId, nyUtbetalingId)
    }

    @Test
    fun `oppretter ny generasjon for vedtaksperiodeId ved ny utbetaling dersom gjeldende generasjon er låst`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), gammelUtbetalingId, varselRepository)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId, varselRepository)

        assertUtbetaling(generasjonId, gammelUtbetalingId)
        assertIkkeUtbetaling(generasjonId, nyUtbetalingId)
        assertGenerasjonFor(gammelUtbetalingId, vedtaksperiodeId)
        assertGenerasjonFor(nyUtbetalingId, vedtaksperiodeId)
    }

    @Test
    fun `kan fjerne utbetalingId fra ulåst generasjon`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        assertUtbetaling(generasjonId, utbetalingId)
        generasjon.invaliderUtbetaling(utbetalingId)
        assertIkkeUtbetaling(generasjonId, utbetalingId)
    }

    @Test
    fun `kan ikke fjerne utbetalingId fra låst generasjon`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertUtbetaling(generasjonId, utbetalingId)
        generasjon.invaliderUtbetaling(utbetalingId)
        assertUtbetaling(generasjonId, utbetalingId)
    }

    @Test
    fun `godkjenner varsler for alle generasjoner som hører til samme utbetaling`() {
        val generasjonIdV1 = UUID.randomUUID()
        val generasjonIdV2 = UUID.randomUUID()
        val generasjonV1 = nyGenerasjon(generasjonIdV1)
        val generasjonV2 = nyGenerasjon(generasjonIdV2)
        val utbetalingId = UUID.randomUUID()
        generasjonV1.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        generasjonV2.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId, varselRepository)
        generasjonV1.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), varselRepository)
        generasjonV2.håndterRegelverksvarsel(UUID.randomUUID(), UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), varselRepository)

        generasjonV2.håndterGodkjentAvSaksbehandler("EN_IDENT", varselRepository)
        assertUtbetaling(generasjonIdV1, utbetalingId)
        assertUtbetaling(generasjonIdV2, utbetalingId)

        assertVarsler(generasjonIdV1, 1, GODKJENT, SB_EX_1)
        assertVarsler(generasjonIdV2, 1, GODKJENT, SB_EX_2)
    }

    @Test
    fun `referential equals`() {
        val generasjon = Generasjon(UUID.randomUUID(), UUID.randomUUID(), generasjonRepository)
        assertEquals(generasjon, generasjon)
        assertEquals(generasjon.hashCode(), generasjon.hashCode())
    }

    @Test
    fun `structural equals`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        assertEquals(generasjon1, generasjon2)
        assertEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig generasjonIder`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId1, vedtaksperiodeId, generasjonRepository)
        val generasjon2 = Generasjon(generasjonId2, vedtaksperiodeId, generasjonRepository)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellige vedtaksperiodeIder`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId1, generasjonRepository)
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId2, generasjonRepository)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig låst`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        generasjon1.håndterVedtakFattet(UUID.randomUUID())
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig utbetalingId`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        generasjon1.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID(), varselRepository)
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        generasjon2.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID(), varselRepository)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig utbetalingId der én generasjon har null`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        generasjon1.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID(), varselRepository)
        val generasjon2 = Generasjon(generasjonId, vedtaksperiodeId, generasjonRepository)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    private fun nyGenerasjon(id: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()): Generasjon {
        generasjonId = id
        return requireNotNull(generasjonRepository.opprettFørste(vedtaksperiodeId, UUID.randomUUID(), generasjonId))
    }

    private fun assertVarsler(generasjonId: UUID, forventetAntall: Int, status: Status, varselkode: Varselkode) {
        @Language("PostgreSQL")
        val query =
            """SELECT COUNT(1) FROM selve_varsel sv INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
               WHERE svg.unik_id = ? AND sv.status = ? AND sv.kode = ?
            """
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId, status.name, varselkode.name).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    private fun assertVarsler(utbetalingId: UUID, vedtaksperiodeId: UUID, forventetAntall: Int, status: Status, varselkode: Varselkode) {
        @Language("PostgreSQL")
        val query =
            """SELECT COUNT(1) FROM selve_varsel sv INNER JOIN selve_vedtaksperiode_generasjon svg ON sv.generasjon_ref = svg.id
               WHERE svg.utbetaling_id = ? AND svg.vedtaksperiode_id = ? AND sv.status = ? AND sv.kode = ?
            """
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, utbetalingId, vedtaksperiodeId, status.name, varselkode.name).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    private fun assertUtbetaling(generasjonId: UUID, utbetalingId: UUID) {
        @Language("PostgreSQL")
        val query =
            """SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = ? AND utbetaling_id = ?
            """
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId, utbetalingId).map { it.int(1) }.asSingle)
        }
        assertEquals(1, antall)
    }

    private fun assertIkkeUtbetaling(generasjonId: UUID, utbetalingId: UUID) {
        @Language("PostgreSQL")
        val query =
            """SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon svg WHERE svg.unik_id = ? AND utbetaling_id = ?
            """
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, generasjonId, utbetalingId).map { it.int(1) }.asSingle)
        }
        assertEquals(0, antall)
    }

    private fun assertGenerasjonFor(utbetalingId: UUID, vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query =
            """SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon svg WHERE svg.vedtaksperiode_id = ? AND utbetaling_id = ?
            """
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId, utbetalingId).map { it.int(1) }.asSingle)
        }
        assertEquals(1, antall)
    }

    private fun assertAntallGenerasjoner(forventetAntall: Int, vedtaksperiodeId: UUID) {
        @Language("PostgreSQL")
        val query =
            """SELECT COUNT(1) FROM selve_vedtaksperiode_generasjon svg WHERE svg.vedtaksperiode_id = ?
            """
        val antall = sessionOf(dataSource).use { session ->
            session.run(queryOf(query, vedtaksperiodeId).map { it.int(1) }.asSingle)
        }
        assertEquals(forventetAntall, antall)
    }

    private fun lagVarseldefinisjoner() {
        val varselkoder = Varselkode.values()
        varselkoder.forEach { varselkode ->
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
                    "En tittel for varselkode=${varselkode}",
                    "En forklaring for varselkode=${varselkode}",
                    "En handling for varselkode=${varselkode}",
                    false,
                    LocalDateTime.now()
                ).asUpdate)
        }
    }

    private object Observer: IVedtaksperiodeObserver {
        private class Opprettelse(
            val generasjonId: UUID,
            val vedtaksperiodeId: UUID,
            val hendelseId: UUID,
            val fom: LocalDate?,
            val tom: LocalDate?,
            val skjæringstidspunkt: LocalDate?
        )

        val opprettedeGenerasjoner = mutableMapOf<UUID, Opprettelse>()
        override fun generasjonOpprettet(
            generasjonId: UUID,
            vedtaksperiodeId: UUID,
            hendelseId: UUID,
            fom: LocalDate?,
            tom: LocalDate?,
            skjæringstidspunkt: LocalDate?
        ) {
            opprettedeGenerasjoner[generasjonId] =
                Opprettelse(generasjonId, vedtaksperiodeId, hendelseId, fom, tom, skjæringstidspunkt)
        }

        fun assertOpprettelse(
            forventetGenerasjonId: UUID,
            forventetVedtaksperiodeId: UUID,
            forventetHendelseId: UUID,
            forventetFom: LocalDate?,
            forventetTom: LocalDate?,
            forventetSkjæringstidspunkt: LocalDate?
        ) {
            val opprettelse = opprettedeGenerasjoner[forventetGenerasjonId]
            assertNotNull(opprettelse)
            requireNotNull(opprettelse)
            assertEquals(forventetGenerasjonId, opprettelse.generasjonId)
            assertEquals(forventetVedtaksperiodeId, opprettelse.vedtaksperiodeId)
            assertEquals(forventetHendelseId, opprettelse.hendelseId)
            assertEquals(forventetFom, opprettelse.fom)
            assertEquals(forventetTom, opprettelse.tom)
            assertEquals(forventetSkjæringstidspunkt, opprettelse.skjæringstidspunkt)
        }
    }
}