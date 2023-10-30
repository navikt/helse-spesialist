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
import no.nav.helse.mars
import no.nav.helse.modell.varsel.ActualVarselRepository
import no.nav.helse.modell.varsel.Varsel
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
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnGenerasjon
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterOppdateringer
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

internal class GenerasjonTest: AbstractDatabaseTest() {
    private val varselRepository = ActualVarselRepository(dataSource)
    private val generasjonRepository = ActualGenerasjonRepository(dataSource)
    private lateinit var generasjonId: UUID
    private lateinit var observer: GenerasjonTestObserver

    @BeforeEach
    internal fun beforeEach() {
        lagVarseldefinisjoner()
        observer = GenerasjonTestObserver()
    }

    @Test
    fun `Kan registrere observer`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
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
        generasjon.håndterTidslinjeendring(1.mars, 31.mars, 1.mars, UUID.randomUUID())
        assertEquals(generasjonId, observer.generasjonId)
        assertEquals(1.mars, observer.fom)
        assertEquals(31.mars, observer.tom)
        assertEquals(1.mars, observer.skjæringstidspunkt)
    }

    @Test
    fun `Generasjon er ikke å anse som oppdatert dersom fom, tom og skjæringstidspunkt er lik oppdatering`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = Generasjon(generasjonId, UUID.randomUUID(), 1.januar, 31.januar, 1.januar)
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
        generasjon.håndterTidslinjeendring(1.januar, 31.januar, 1.januar, UUID.randomUUID())
        assertNull(observer.generasjonId)
        assertNull(observer.fom)
        assertNull(observer.tom)
        assertNull(observer.skjæringstidspunkt)
    }

    @Test
    fun `Generasjon skal ikke oppdateres dersom den er ferdig behandlet`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.håndterVedtakFattet(UUID.randomUUID())
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
        generasjon.håndterTidslinjeendring(1.januar, 31.januar, 1.januar, UUID.randomUUID())
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
        generasjon.håndterTidslinjeendring(1.januar, 31.januar, 1.januar, UUID.randomUUID())
        assertEquals(
            Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar),
            generasjon
        )
    }

    @Test
    fun `håndterTidslinjeendring setter ikke fom, tom og skjæringstidspunkt på generasjonen hvis den er ferdig behandlet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, vedtaksperiodeId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterTidslinjeendring(1.februar, 28.februar, 1.februar, UUID.randomUUID())
        val forventetGenerasjon = Generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        forventetGenerasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(
            forventetGenerasjon,
            generasjon
        )
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
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndterVedtaksperiodeOpprettet(UUID.randomUUID())
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
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndterVedtaksperiodeOpprettet(UUID.randomUUID())
        generasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId, VURDERT),
            UUID.randomUUID()
        )
        generasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId, VURDERT),
            UUID.randomUUID()
        )
        assertTrue(generasjon.forhindrerAutomatisering())
    }

    @Test
    fun `opprett neste`() {
        val nyGenerasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = Generasjon(UUID.randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        val hendelseId = UUID.randomUUID()
        generasjon.registrer(observer)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nyGenerasjon = generasjon.håndterVedtaksperiodeEndret(hendelseId, nyGenerasjonId)
        assertEquals(1, observer.opprettedeGenerasjoner.size)
        assertNotEquals(generasjon, nyGenerasjon)
        observer.assertOpprettelse(nyGenerasjonId, vedtaksperiodeId, hendelseId, 1.januar, 31.januar, 1.januar)
        assertEquals(
            Generasjon(nyGenerasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar),
            nyGenerasjon
        )
    }

    @Test
    fun `ikke opprett neste dersom nåværende er ubehandlet`() {
        val nyGenerasjonId = UUID.randomUUID()
        val generasjon = generasjon(UUID.randomUUID(), UUID.randomUUID())
        generasjon.registrer(observer)
        val nyGenerasjon = generasjon.håndterVedtaksperiodeEndret(UUID.randomUUID(), nyGenerasjonId)
        assertEquals(0, observer.opprettedeGenerasjoner.size)
        assertNull(nyGenerasjon)
    }

    @Test
    fun `Kopierer skjæringstidspunkt og periode til neste generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterTidslinjeendring(
            fom = 1.januar,
            tom = 5.januar,
            skjæringstidspunkt = 1.januar,
            hendelseId = UUID.randomUUID()
        )
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nyGenerasjonId = UUID.randomUUID()
        val nyGenerasjon = generasjon.håndterVedtaksperiodeEndret(UUID.randomUUID(), nyGenerasjonId)
        val forventetGenerasjon = Generasjon(nyGenerasjonId, vedtaksperiodeId, 1.januar, 5.januar, 1.januar)

        assertEquals(forventetGenerasjon, nyGenerasjon)
    }

    @Test
    fun `flytter aktive varsler til neste generasjon når den opprettes`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nyGenerasjonId = UUID.randomUUID()
        generasjon.håndterVedtaksperiodeEndret(UUID.randomUUID(), nyGenerasjonId)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(nyGenerasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `flytter ikke varsler som har en annen status enn aktiv`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId, INAKTIV),
            UUID.randomUUID()
        )
        generasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId, GODKJENT),
            UUID.randomUUID()
        )
        generasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_3", LocalDateTime.now(), vedtaksperiodeId, AVVIST),
            UUID.randomUUID()
        )
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nyGenerasjonId = UUID.randomUUID()
        generasjon.håndterVedtaksperiodeEndret(UUID.randomUUID(), nyGenerasjonId)
        assertVarsler(nyGenerasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(nyGenerasjonId, 0, INAKTIV, SB_EX_1)
        assertVarsler(nyGenerasjonId, 0, GODKJENT, SB_EX_2)
        assertVarsler(nyGenerasjonId, 0, AVVIST, SB_EX_3)
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
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId)

        assertIkkeUtbetaling(generasjonId, gammelUtbetalingId)
        assertUtbetaling(generasjonId, nyUtbetalingId)
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

        assertUtbetaling(generasjonId, gammelUtbetalingId)
        assertIkkeUtbetaling(generasjonId, nyUtbetalingId)
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
    fun `oppretter ny generasjon for vedtaksperiodeId ved ny utbetaling dersom gjeldende generasjon er ferdig behandlet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), gammelUtbetalingId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId)

        assertUtbetaling(generasjonId, gammelUtbetalingId)
        assertIkkeUtbetaling(generasjonId, nyUtbetalingId)
        assertGenerasjonFor(gammelUtbetalingId, vedtaksperiodeId)
        assertGenerasjonFor(nyUtbetalingId, vedtaksperiodeId)
    }

    @Test
    fun `oppdaterer tidslinje på ubehandlet generasjon der fom, tom og eller skjæringstidspunkt er ulike`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = nyGenerasjon(generasjonId)
        generasjon.registrer(observer)
        generasjon.håndterTidslinjeendring(1.mars, 31.mars, 1.mars, UUID.randomUUID())
        observer.assertTidslinjeendring(generasjonId, 1.mars, 31.mars, 1.mars)
    }

    @Test
    fun `oppdaterer ikke tidslinje på ubehandlet generasjon der fom, tom og eller skjæringstidspunkt er like`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = nyGenerasjon(generasjonId)
        generasjon.registrer(observer)
        generasjon.håndterTidslinjeendring(1.januar, 31.januar, 1.januar, UUID.randomUUID())
        assertEquals(0, observer.oppdaterteGenerasjoner.size)
    }

    @Test
    fun `oppretter ny generasjon dersom gjeldende generasjon er ferdig behandlet og tidslinje er ulik`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val hendelseId = UUID.randomUUID()
        val generasjon = nyGenerasjon(generasjonId, vedtaksperiodeId)
        generasjon.registrer(observer)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterTidslinjeendring(2.januar, 31.januar, 2.januar, hendelseId)
        assertEquals(1, observer.opprettedeGenerasjoner.size)
        observer.assertOpprettelse(vedtaksperiodeId, hendelseId, 2.januar, 31.januar, 2.januar)
    }

    @Test
    fun `kan fjerne utbetalingId fra ubehandlet generasjon`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        assertUtbetaling(generasjonId, utbetalingId)
        generasjon.håndterForkastetUtbetaling(utbetalingId)
        assertIkkeUtbetaling(generasjonId, utbetalingId)
    }

    @Test
    fun `kan ikke fjerne utbetalingId fra ferdig behandlet generasjon`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertUtbetaling(generasjonId, utbetalingId)
        generasjon.håndterForkastetUtbetaling(utbetalingId)
        assertUtbetaling(generasjonId, utbetalingId)
    }

    @Test
    fun `håndterer oppdateringer`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonV1 = generasjon(generasjonId1, vedtaksperiodeId1)
        val generasjonV2 = generasjon(generasjonId2, vedtaksperiodeId2)

        val observer = object : IVedtaksperiodeObserver {
            val oppdaterteGenerasjoner = mutableListOf<UUID>()
            override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
                oppdaterteGenerasjoner.add(generasjonId)
            }
        }
        generasjonV1.registrer(observer)
        generasjonV2.registrer(observer)

        listOf(generasjonV1, generasjonV2).håndterOppdateringer(
            listOf(
                VedtaksperiodeOppdatering(1.mars, 31.mars, 1.mars, vedtaksperiodeId1),
                VedtaksperiodeOppdatering(1.mars, 31.mars, 1.mars, vedtaksperiodeId2),
            ),
            UUID.randomUUID()
        )
        assertEquals(2, observer.oppdaterteGenerasjoner.size)
        assertEquals(generasjonId1, observer.oppdaterteGenerasjoner[0])
        assertEquals(generasjonId2, observer.oppdaterteGenerasjoner[1])
    }

    @Test
    fun `finn generasjon`() {
        val vedtaksperiodeId1 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonV1 = generasjon(vedtaksperiodeId = vedtaksperiodeId1)
        val generasjonV2 = generasjon(vedtaksperiodeId = vedtaksperiodeId2)

        assertNotNull(listOf(generasjonV1, generasjonV2).finnGenerasjon(vedtaksperiodeId1))
    }

    @Test
    fun `finner ikke generasjon`() {
        val generasjonV1 = generasjon()
        assertNull(listOf(generasjonV1).finnGenerasjon(UUID.randomUUID()))
    }

    @Test
    fun `håndterer oppdateringer for kun noen av vedtaksperiodene`() {
        val vedtaksperiodeId2 = UUID.randomUUID()
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonV1 = generasjon(generasjonId1, UUID.randomUUID())
        val generasjonV2 = generasjon(generasjonId2, vedtaksperiodeId2)

        val observer = object : IVedtaksperiodeObserver {
            val oppdaterteGenerasjoner = mutableListOf<UUID>()
            override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
                oppdaterteGenerasjoner.add(generasjonId)
            }
        }
        generasjonV1.registrer(observer)
        generasjonV2.registrer(observer)

        listOf(generasjonV1, generasjonV2).håndterOppdateringer(
            listOf(VedtaksperiodeOppdatering(1.mars, 31.mars, 1.mars, vedtaksperiodeId2)),
            UUID.randomUUID()
        )
        assertEquals(1, observer.oppdaterteGenerasjoner.size)
        assertEquals(generasjonId2, observer.oppdaterteGenerasjoner[0])
    }

    @Test
    fun `håndterer ikke oppdateringer for noen av vedtaksperiodene`() {
        val generasjonId1 = UUID.randomUUID()
        val generasjonId2 = UUID.randomUUID()
        val generasjonV1 = generasjon(generasjonId1, UUID.randomUUID())
        val generasjonV2 = generasjon(generasjonId2, UUID.randomUUID())

        val observer = object : IVedtaksperiodeObserver {
            val oppdaterteGenerasjoner = mutableListOf<UUID>()
            override fun tidslinjeOppdatert(generasjonId: UUID, fom: LocalDate, tom: LocalDate, skjæringstidspunkt: LocalDate) {
                oppdaterteGenerasjoner.add(generasjonId)
            }
        }
        generasjonV1.registrer(observer)
        generasjonV2.registrer(observer)

        listOf(generasjonV1, generasjonV2).håndterOppdateringer(
            listOf(VedtaksperiodeOppdatering(1.januar, 31.januar, 1.januar, UUID.randomUUID())),
            UUID.randomUUID()
        )
        assertEquals(0, observer.oppdaterteGenerasjoner.size)
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
        val generasjon1 = generasjonMedVarsel(varselkode =  "RV_UT_23")
        assertTrue(generasjon1.hasterÅBehandle())
    }

    @Test
    fun `krever skjønnsfastsettelse hvis generasjon har varsel om avvik`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = listOf(generasjonMedVarsel(vedtaksperiodeId = vedtaksperiodeId, varselkode =  "RV_IV_2"))
        assertTrue(generasjon1.kreverSkjønnsfastsettelse(vedtaksperiodeId = vedtaksperiodeId))
    }

    @Test
    fun `haster ikke å behandle hvis generasjonen ikke har varsel om negativt beløp`() {
        val generasjon1 = generasjon()
        assertFalse(generasjon1.hasterÅBehandle())
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
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId)
        generasjon1.håndterVedtakFattet(UUID.randomUUID())
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId)
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
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId, 1.januar, 31.januar)
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId, 1.februar, 28.februar)
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
        generasjon1.håndterVedtakFattet(UUID.randomUUID())
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId, skjæringstidspunkt = 1.januar)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    private fun nyGenerasjon(id: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()): Generasjon {
        generasjonId = id
        val generasjon = Generasjon(id, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        generasjon.registrer(generasjonRepository)
        generasjon.håndterVedtaksperiodeOpprettet(UUID.randomUUID())
        return generasjon
    }


    private fun generasjon(
        generasjonId: UUID = UUID.randomUUID(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = 1.januar,
    ) = Generasjon(
        id = generasjonId,
        vedtaksperiodeId = vedtaksperiodeId,
        fom = fom,
        tom = tom,
        skjæringstidspunkt = skjæringstidspunkt
    ).also {
        it.registrer(observer)
    }

    @Test
    fun `generasjon toDto`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val fom = 1.januar
        val tom = 31. januar
        val skjæringstidspunkt = 1.januar
        val utbetalingId = UUID.randomUUID()
        val generasjon = Generasjon(generasjonId, vedtaksperiodeId, fom, tom, skjæringstidspunkt)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)

        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val varsel = Varsel(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, AKTIV)
        generasjon.håndterNyttVarsel(varsel, UUID.randomUUID())
        val dto = generasjon.toDto()

        assertEquals(GenerasjonDto(generasjonId, vedtaksperiodeId, utbetalingId, skjæringstidspunkt, fom til tom, TilstandDto.Ulåst, listOf(varsel.toDto())), dto)
    }

    @Test
    fun `generasjonTilstand toDto`() {
        assertEquals(TilstandDto.Låst, Generasjon.Låst.toDto())
        assertEquals(TilstandDto.Ulåst, Generasjon.Ulåst.toDto())
        assertEquals(TilstandDto.AvsluttetUtenUtbetaling, Generasjon.AvsluttetUtenUtbetaling.toDto())
        assertEquals(TilstandDto.UtenUtbetalingMåVurderes, Generasjon.UtenUtbetalingMåVurderes.toDto())
    }

    private fun generasjonMedVarsel(fom: LocalDate = 1.januar, tom: LocalDate = 31.januar, vedtaksperiodeId: UUID = UUID.randomUUID(), varselkode: String = "SB_EX_1"): Generasjon {
        return generasjon(vedtaksperiodeId = vedtaksperiodeId, fom = fom, tom = tom).also {
            it.håndterNyttVarsel(Varsel(UUID.randomUUID(), varselkode, LocalDateTime.now(), vedtaksperiodeId), UUID.randomUUID())
            it.registrer(observer)
        }
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
                    "En tittel for varselkode=${varselkode}",
                    "En forklaring for varselkode=${varselkode}",
                    "En handling for varselkode=${varselkode}",
                    false,
                    LocalDateTime.now()
                ).asUpdate)
        }
    }
}
