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
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterAvvist
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterGodkjent
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.håndterOppdateringer
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.kreverTotrinnsvurdering
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
    private lateinit var observer: Observer

    @BeforeEach
    internal fun beforeEach() {
        lagVarseldefinisjoner()
        observer = Observer()
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
    fun `Generasjon skal ikke oppdateres dersom den er låst`() {
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
    fun `håndterTidslinjeendring setter ikke fom, tom og skjæringstidspunkt på generasjonen hvis den er låst`() {
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
        generasjon.opprettFørste(UUID.randomUUID())
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
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
        generasjon.opprettFørste(UUID.randomUUID())
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId, VURDERT))
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId, VURDERT))
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
        val nyGenerasjon = generasjon.håndterNyGenerasjon(hendelseId, nyGenerasjonId)
        assertEquals(1, observer.opprettedeGenerasjoner.size)
        assertNotEquals(generasjon, nyGenerasjon)
        observer.assertOpprettelse(nyGenerasjonId, vedtaksperiodeId, hendelseId, 1.januar, 31.januar, 1.januar)
        assertEquals(
            Generasjon(nyGenerasjonId, vedtaksperiodeId, 1.januar, 31.januar, 1.januar),
            nyGenerasjon
        )
    }

    @Test
    fun `ikke opprett neste dersom nåværende er ulåst`() {
        val nyGenerasjonId = UUID.randomUUID()
        val generasjon = generasjon(UUID.randomUUID(), UUID.randomUUID())
        generasjon.registrer(observer)
        val nyGenerasjon = generasjon.håndterNyGenerasjon(UUID.randomUUID(), nyGenerasjonId)
        assertEquals(0, observer.opprettedeGenerasjoner.size)
        assertNull(nyGenerasjon)
    }

    @Test
    fun `endrer tilstand etter vedtak fattet`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(generasjonId, UUID.randomUUID())
        generasjon.registrer(observer)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertEquals(1, observer.låsteGenerasjoner.size)
        observer.assertTilstandsendring(generasjonId, Generasjon.Ulåst, Generasjon.Låst, 0)
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
        val nyGenerasjon = generasjon.håndterNyGenerasjon(UUID.randomUUID(), nyGenerasjonId)
        val forventetGenerasjon = Generasjon(nyGenerasjonId, vedtaksperiodeId, 1.januar, 5.januar, 1.januar)

        assertEquals(forventetGenerasjon, nyGenerasjon)
    }

    @Test
    fun `flytter aktive varsler til neste generasjon når den opprettes`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nyGenerasjonId = UUID.randomUUID()
        generasjon.håndterNyGenerasjon(UUID.randomUUID(), nyGenerasjonId)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(nyGenerasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `flytter ikke varsler som har en annen status enn aktiv`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId, INAKTIV))
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId, GODKJENT))
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_3", LocalDateTime.now(), vedtaksperiodeId, AVVIST))
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val nyGenerasjonId = UUID.randomUUID()
        generasjon.håndterNyGenerasjon(UUID.randomUUID(), nyGenerasjonId)
        assertVarsler(nyGenerasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(nyGenerasjonId, 0, INAKTIV, SB_EX_1)
        assertVarsler(nyGenerasjonId, 0, GODKJENT, SB_EX_2)
        assertVarsler(nyGenerasjonId, 0, AVVIST, SB_EX_3)
    }

    @Test
    fun `godkjenner enkelt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.håndterGodkjentAvSaksbehandler("EN_IDENT")
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, GODKJENT, SB_EX_1)
    }

    @Test
    fun `deaktiverer enkelt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndter(varsel)
        generasjon.håndterDeaktivertVarsel(varsel)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)
    }
    @Test
    fun `godkjenner alle varsler når generasjonen blir godkjent`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), UUID.randomUUID())
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.håndterGodkjentAvSaksbehandler("EN_IDENT")
        assertVarsler(generasjonId, 1, GODKJENT, SB_EX_1)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, GODKJENT, SB_EX_2)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_2)
    }

    @Test
    fun `avviser alle varsler når generasjonen blir avvist`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.håndterAvvistAvSaksbehandler("EN_IDENT")
        assertVarsler(generasjonId, 1, AVVIST, SB_EX_1)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, AVVIST, SB_EX_2)
        assertVarsler(generasjonId, 0, AKTIV, SB_EX_2)
    }

    @Test
    fun `Lagrer kun én utgave av et aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))

        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `kan reaktivere deaktivert varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndter(varsel)
        generasjon.håndterDeaktivertVarsel(varsel)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)

        generasjon.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))

        assertVarsler(generasjonId, 0, INAKTIV, SB_EX_1)
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `kan deaktivere reaktivert varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndter(varsel)
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
        generasjon.håndterDeaktivertVarsel(varsel)
        assertVarsler(generasjonId, 1, INAKTIV, SB_EX_1)
        generasjon.håndter(varsel)
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
    fun `Generasjon kan motta ny utbetalingId så lenge generasjonen ikke er låst`() {
        val generasjon = nyGenerasjon()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), gammelUtbetalingId)
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId)

        assertIkkeUtbetaling(generasjonId, gammelUtbetalingId)
        assertUtbetaling(generasjonId, nyUtbetalingId)
    }

    @Test
    fun `Lagrer varsel på generasjon selvom den er låst`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId, id = generasjonId)
        generasjon.registrer(observer)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        val varsel = Varsel(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndter(varsel)

        assertEquals(1, observer.opprettedeVarsler[generasjonId]?.size)
        assertEquals("RV_IM_1", observer.opprettedeVarsler[generasjonId]?.get(0))
        assertAntallGenerasjoner(1, vedtaksperiodeId)
    }

    @Test
    fun `Skal kunne opprette varsel på generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = nyGenerasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.registrer(generasjonRepository, varselRepository)
        generasjon.håndter(Varsel(UUID.randomUUID(), SB_EX_1.name, LocalDateTime.now(), vedtaksperiodeId))
        assertAntallGenerasjoner(1, vedtaksperiodeId)
        assertVarsler(generasjonId, 1, AKTIV, SB_EX_1)
    }

    @Test
    fun `kan ikke knytte utbetalingId til låst generasjon som har utbetalingId fra før`() {
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
    fun `kan ikke knytte utbetalingId til låst generasjon som ikke har utbetalingId fra før`() {
        val generasjon = nyGenerasjon()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), nyUtbetalingId)
        assertIkkeUtbetaling(generasjonId, nyUtbetalingId)
    }

    @Test
    fun `oppretter ny generasjon for vedtaksperiodeId ved ny utbetaling dersom gjeldende generasjon er låst`() {
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
    fun `oppdaterer tidslinje på ulåst generasjon der fom, tom og eller skjæringstidspunkt er ulike`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = nyGenerasjon(generasjonId)
        generasjon.registrer(observer)
        generasjon.håndterTidslinjeendring(1.mars, 31.mars, 1.mars, UUID.randomUUID())
        observer.assertTidslinjeendring(generasjonId, 1.mars, 31.mars, 1.mars)
    }

    @Test
    fun `oppdaterer ikke tidslinje på ulåst generasjon der fom, tom og eller skjæringstidspunkt er like`() {
        val generasjonId = UUID.randomUUID()
        val generasjon = nyGenerasjon(generasjonId)
        generasjon.registrer(observer)
        generasjon.håndterTidslinjeendring(1.januar, 31.januar, 1.januar, UUID.randomUUID())
        assertEquals(0, observer.oppdaterteGenerasjoner.size)
    }

    @Test
    fun `oppretter ny generasjon dersom gjeldende generasjon er låst og tidslinje er ulik`() {
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
    fun `kan fjerne utbetalingId fra ulåst generasjon`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        assertUtbetaling(generasjonId, utbetalingId)
        generasjon.invaliderUtbetaling(utbetalingId)
        assertIkkeUtbetaling(generasjonId, utbetalingId)
    }

    @Test
    fun `kan ikke fjerne utbetalingId fra låst generasjon`() {
        val generasjon = nyGenerasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        generasjon.håndterVedtakFattet(UUID.randomUUID())
        assertUtbetaling(generasjonId, utbetalingId)
        generasjon.invaliderUtbetaling(utbetalingId)
        assertUtbetaling(generasjonId, utbetalingId)
    }

    @Test
    fun `godkjenner varsler for generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonIdV1 = UUID.randomUUID()
        val generasjonV1 = nyGenerasjon(generasjonIdV1, vedtaksperiodeId)
        generasjonV1.registrer(generasjonRepository, varselRepository)
        val utbetalingId = UUID.randomUUID()
        generasjonV1.håndterNyUtbetaling(UUID.randomUUID(), utbetalingId)
        generasjonV1.håndter(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))

        generasjonV1.håndterGodkjentAvSaksbehandler("EN_IDENT")
        assertUtbetaling(generasjonIdV1, utbetalingId)
        assertVarsler(generasjonIdV1, 1, GODKJENT, SB_EX_1)
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
    fun `håndter godkjent periode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(1.januar, 31.januar, vedtaksperiodeId)
        val generasjon2 = generasjonMedVarsel(1.januar, 31.januar)
        listOf(generasjon1, generasjon2).håndterGodkjent("Ident", vedtaksperiodeId)
        assertEquals(2, observer.godkjenteVarsler.size)
    }

    @Test
    fun `håndter avvist periode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(1.januar, 31.januar, vedtaksperiodeId)
        val generasjon2 = generasjonMedVarsel(1.januar, 31.januar)
        listOf(generasjon1, generasjon2).håndterAvvist("Ident", vedtaksperiodeId)
        assertEquals(2, observer.avvisteVarsler.size)
    }

    @Test
    fun `ikke godkjenn varsler for perioder som ligger etter i tid`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(1.januar, 31.januar, vedtaksperiodeId)
        val generasjon2 = generasjonMedVarsel(1.februar, 28.februar)
        listOf(generasjon1, generasjon2).håndterGodkjent("Ident", vedtaksperiodeId)
        assertEquals(1, observer.godkjenteVarsler.size)
    }

    @Test
    fun `godkjenn varsler for perioder som overlapper med én dag`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(1.januar, 31.januar, vedtaksperiodeId)
        val generasjon2 = generasjonMedVarsel(31.januar, 28.februar)
        listOf(generasjon1, generasjon2).håndterGodkjent("Ident", vedtaksperiodeId)
        assertEquals(2, observer.godkjenteVarsler.size)
    }

    @Test
    fun `godkjenn varsler for perioder som ligger helt før`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(1.februar, 28.februar, vedtaksperiodeId)
        val generasjon2 = generasjonMedVarsel(1.januar, 31.januar)
        listOf(generasjon1, generasjon2).håndterGodkjent("Ident", vedtaksperiodeId)
        assertEquals(2, observer.godkjenteVarsler.size)
    }

    @Test
    fun `godkjenn varsler for perioder som overlapper delvis i starten`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(1.februar, 28.februar, vedtaksperiodeId)
        val generasjon2 = generasjonMedVarsel(16.januar, 15.februar)
        listOf(generasjon1, generasjon2).håndterGodkjent("Ident", vedtaksperiodeId)
        assertEquals(2, observer.godkjenteVarsler.size)
    }

    @Test
    fun `godkjenn varsler for perioder som overlapper delvis i slutten`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(1.februar, 28.februar, vedtaksperiodeId)
        val generasjon2 = generasjonMedVarsel(16.februar, 15.mars)
        listOf(generasjon1, generasjon2).håndterGodkjent("Ident", vedtaksperiodeId)
        assertEquals(2, observer.godkjenteVarsler.size)
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
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId)
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
    fun `forskjellig låst`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId)
        generasjon1.håndterVedtakFattet(UUID.randomUUID())
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId)
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

    private fun nyGenerasjon(id: UUID = UUID.randomUUID(), vedtaksperiodeId: UUID = UUID.randomUUID()): Generasjon {
        generasjonId = id
        val generasjon = Generasjon(id, vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
        generasjon.registrer(generasjonRepository)
        generasjon.opprettFørste(UUID.randomUUID())
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
    )

    private fun generasjonMedVarsel(fom: LocalDate, tom: LocalDate, vedtaksperiodeId: UUID = UUID.randomUUID(), varselkode: String = "SB_EX_1"): Generasjon {
        return generasjon(vedtaksperiodeId = vedtaksperiodeId, fom = fom, tom = tom).also {
            it.håndter(Varsel(UUID.randomUUID(), varselkode, LocalDateTime.now(), vedtaksperiodeId))
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

    private class Observer: IVedtaksperiodeObserver {
        private class Tidslinjeendring(
            val fom: LocalDate,
            val tom: LocalDate,
            val skjæringstidspunkt: LocalDate
        )

        private class Opprettelse(
            val generasjonId: UUID,
            val vedtaksperiodeId: UUID,
            val hendelseId: UUID,
            val fom: LocalDate?,
            val tom: LocalDate?,
            val skjæringstidspunkt: LocalDate?
        )

        val låsteGenerasjoner = mutableListOf<UUID>()
        val tilstandsendringer = mutableMapOf<UUID, MutableList<Pair<Generasjon.Tilstand, Generasjon.Tilstand>>>()
        val opprettedeGenerasjoner = mutableMapOf<UUID, Opprettelse>()
        val oppdaterteGenerasjoner = mutableMapOf<UUID, Tidslinjeendring>()
        val opprettedeVarsler = mutableMapOf<UUID, MutableList<String>>()
        val godkjenteVarsler = mutableListOf<UUID>()
        val avvisteVarsler = mutableListOf<UUID>()

        override fun vedtakFattet(generasjonId: UUID, hendelseId: UUID) {
            låsteGenerasjoner.add(generasjonId)
        }

        override fun tilstandEndret(
            generasjonId: UUID,
            vedtaksperiodeId: UUID,
            gammel: Generasjon.Tilstand,
            ny: Generasjon.Tilstand
        ) {
            tilstandsendringer.getOrPut(generasjonId) { mutableListOf() }.add(gammel to ny)
        }
        override fun generasjonOpprettet(
            generasjonId: UUID,
            vedtaksperiodeId: UUID,
            hendelseId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate
        ) {
            opprettedeGenerasjoner[generasjonId] =
                Opprettelse(generasjonId, vedtaksperiodeId, hendelseId, fom, tom, skjæringstidspunkt)
        }

        override fun tidslinjeOppdatert(
            generasjonId: UUID,
            fom: LocalDate,
            tom: LocalDate,
            skjæringstidspunkt: LocalDate
        ) {
           oppdaterteGenerasjoner[generasjonId] = Tidslinjeendring(fom, tom, skjæringstidspunkt)
        }

        override fun varselOpprettet(
            varselId: UUID,
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselkode: String,
            opprettet: LocalDateTime
        ) {
            opprettedeVarsler.getOrPut(generasjonId) { mutableListOf() }.add(varselkode)
        }

        override fun varselGodkjent(
            varselId: UUID,
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselkode: String,
            ident: String
        ) {
            godkjenteVarsler.add(varselId)
        }

        override fun varselAvvist(
            varselId: UUID,
            vedtaksperiodeId: UUID,
            generasjonId: UUID,
            varselkode: String,
            ident: String
        ) {
            avvisteVarsler.add(varselId)
        }

        fun assertTilstandsendring(
            generasjonId: UUID,
            forventetGammel: Generasjon.Tilstand,
            forventetNy: Generasjon.Tilstand,
            index: Int
        ) {
            val (gammel, ny) = tilstandsendringer[generasjonId]!![index]
            assertEquals(forventetGammel, gammel)
            assertEquals(forventetNy, ny)
        }

        fun assertTidslinjeendring(
            generasjonId: UUID,
            forventetFom: LocalDate,
            forventetTom: LocalDate,
            forventetSkjæringstidspunkt: LocalDate
        ) {
            val tidslinjeendring = oppdaterteGenerasjoner[generasjonId]
            assertEquals(forventetFom, tidslinjeendring?.fom)
            assertEquals(forventetTom, tidslinjeendring?.tom)
            assertEquals(forventetSkjæringstidspunkt, tidslinjeendring?.skjæringstidspunkt)
        }

        fun assertOpprettelse(
            forventetGenerasjonId: UUID,
            forventetVedtaksperiodeId: UUID,
            forventetHendelseId: UUID,
            forventetFom: LocalDate,
            forventetTom: LocalDate,
            forventetSkjæringstidspunkt: LocalDate
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

        fun assertOpprettelse(
            forventetVedtaksperiodeId: UUID,
            forventetHendelseId: UUID,
            forventetFom: LocalDate,
            forventetTom: LocalDate,
            forventetSkjæringstidspunkt: LocalDate
        ) {
            val opprettelser = opprettedeGenerasjoner.values.filter { it.vedtaksperiodeId == forventetVedtaksperiodeId }
            assertEquals(1, opprettelser.size)
            val opprettelse = opprettelser[0]
            assertEquals(forventetVedtaksperiodeId, opprettelse.vedtaksperiodeId)
            assertEquals(forventetHendelseId, opprettelse.hendelseId)
            assertEquals(forventetFom, opprettelse.fom)
            assertEquals(forventetTom, opprettelse.tom)
            assertEquals(forventetSkjæringstidspunkt, opprettelse.skjæringstidspunkt)
        }
    }
}