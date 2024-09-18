package no.nav.helse.modell.vedtaksperiode

import io.mockk.mockk
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.person.vedtaksperiode.Varsel
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.AKTIV
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Status.VURDERT
import no.nav.helse.modell.person.vedtaksperiode.VarselStatusDto
import no.nav.helse.modell.varsel.Varselkode
import no.nav.helse.modell.varsel.Varselkode.SB_EX_1
import no.nav.helse.modell.vedtak.AvsluttetUtenVedtak
import no.nav.helse.modell.vedtak.SykepengevedtakBuilder
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnGenerasjonForSpleisBehandling
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnGenerasjonForVedtaksperiode
import no.nav.helse.modell.vedtaksperiode.Generasjon.Companion.finnSisteGenerasjonUtenSpleisBehandlingId
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

internal class GenerasjonTest {
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
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        assertTrue(generasjon.forhindrerAutomatisering())
    }

    @Test
    fun `generasjon har kun gosysvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        assertTrue(generasjon.harKunGosysvarsel())
    }

    @Test
    fun `generasjon har ingen varsler og dermed ikke kun gosysvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        assertFalse(generasjon.harKunGosysvarsel())
    }

    @Test
    fun `generasjon har flere varsler og dermed ikke kun gosysvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId))
        assertFalse(generasjon.harKunGosysvarsel())
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
        )
        generasjon.håndterNyttVarsel(
            Varsel(UUID.randomUUID(), "SB_EX_2", LocalDateTime.now(), vedtaksperiodeId, VURDERT),
        )
        assertTrue(generasjon.forhindrerAutomatisering())
    }

    @Test
    fun `deaktiverer enkelt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndterNyttVarsel(varsel)
        generasjon.håndterDeaktivertVarsel(varsel)
        generasjon.assertVarsler(0, VarselStatusDto.AKTIV, SB_EX_1)
        generasjon.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)
    }

    @Test
    fun `deaktiverer enkelt varsel basert på varselkode`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndterNyttVarsel(varsel)
        generasjon.deaktiverVarsel("SB_EX_1")
        generasjon.assertVarsler(0, VarselStatusDto.AKTIV, SB_EX_1)
        generasjon.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)
    }

    @Test
    fun `sletter varsel om avvik og legger det til på nytt hvis det finnes fra før`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val varselId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "RV_IV_2", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.håndterNyttVarsel(Varsel(varselId, "RV_IV_2", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.assertVarsler(1, VarselStatusDto.AKTIV, "RV_IV_2")
    }

    @Test
    fun `Lagrer kun én utgave av et aktivt varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))

        generasjon.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
    }

    @Test
    fun `kan reaktivere deaktivert varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndterNyttVarsel(varsel)
        generasjon.håndterDeaktivertVarsel(varsel)
        generasjon.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)

        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId))

        generasjon.assertVarsler(0, VarselStatusDto.INAKTIV, SB_EX_1)
        generasjon.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
    }

    @Test
    fun `kan deaktivere reaktivert varsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        val varsel = Varsel(UUID.randomUUID(), "SB_EX_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndterNyttVarsel(varsel)
        generasjon.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
        generasjon.håndterDeaktivertVarsel(varsel)
        generasjon.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)
        generasjon.håndterNyttVarsel(varsel)
        generasjon.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
        generasjon.håndterDeaktivertVarsel(varsel)

        generasjon.assertVarsler(1, VarselStatusDto.INAKTIV, SB_EX_1)
        generasjon.assertVarsler(0, VarselStatusDto.AKTIV, SB_EX_1)
    }

    @Test
    fun `Generasjon kan motta ny utbetalingId`() {
        val generasjon = generasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(utbetalingId)
    }

    @Test
    fun `Generasjon kan motta ny utbetalingId så lenge generasjonen ikke er ferdig behandlet`() {
        val generasjon = generasjon()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(gammelUtbetalingId)
        generasjon.håndterForkastetUtbetaling(gammelUtbetalingId)
        generasjon.håndterNyUtbetaling(nyUtbetalingId)

        assertEquals(nyUtbetalingId, generasjon.toDto().utbetalingId)
    }

    @Test
    fun `Lagrer varsel på generasjon selvom den er ferdig behandlet`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjonId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId, generasjonId = generasjonId)
        generasjon.håndterVedtakFattet()
        val varsel = Varsel(UUID.randomUUID(), "RV_IM_1", LocalDateTime.now(), vedtaksperiodeId)
        generasjon.håndterNyttVarsel(varsel)
        generasjon.assertVarsler(1, VarselStatusDto.AKTIV, "RV_IM_1")
    }

    @Test
    fun `Skal kunne opprette varsel på generasjon`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjon(vedtaksperiodeId = vedtaksperiodeId)
        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), SB_EX_1.name, LocalDateTime.now(), vedtaksperiodeId))
        generasjon.assertVarsler(1, VarselStatusDto.AKTIV, SB_EX_1)
    }

    @Test
    fun `kan ikke knytte utbetalingId til ferdig behandlet generasjon som har utbetalingId fra før`() {
        val generasjon = generasjon()
        val gammelUtbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(gammelUtbetalingId)
        generasjon.håndterVedtakFattet()
        generasjon.håndterNyUtbetaling(nyUtbetalingId)

        assertEquals(gammelUtbetalingId, generasjon.toDto().utbetalingId)
    }

    @Test
    fun `kan ikke knytte utbetalingId til ferdig behandlet generasjon som ikke har utbetalingId fra før`() {
        val utbetalingId = UUID.randomUUID()
        val nyUtbetalingId = UUID.randomUUID()

        val generasjon = generasjon()
        generasjon.håndterNyUtbetaling(utbetalingId)

        generasjon.håndterVedtakFattet()
        generasjon.håndterNyUtbetaling(nyUtbetalingId)
        generasjon.assertUtbetalingId(utbetalingId)
    }

    @Test
    fun `kan fjerne utbetalingId fra ubehandlet generasjon`() {
        val generasjon = generasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(utbetalingId)
        generasjon.assertUtbetalingId(utbetalingId)
        generasjon.håndterForkastetUtbetaling(utbetalingId)
        generasjon.assertUtbetalingId(null)
    }

    @Test
    fun `kan ikke fjerne utbetalingId fra ferdig behandlet generasjon`() {
        val generasjon = generasjon()
        val utbetalingId = UUID.randomUUID()
        generasjon.håndterNyUtbetaling(utbetalingId)
        generasjon.håndterVedtakFattet()
        generasjon.håndterForkastetUtbetaling(utbetalingId)
        generasjon.assertUtbetalingId(utbetalingId)
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
    fun `har generasjon medlemskapsvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(1.februar, 28.februar, vedtaksperiodeId, "RV_MV_1")
        assertTrue(generasjon1.harMedlemskapsvarsel())
    }

    @Test
    fun `har kun åpen oppgave i gosys`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjonMedVarsel(1.februar, 28.februar, vedtaksperiodeId, "SB_EX_1")
        assertTrue(generasjon.harKunGosysvarsel())
    }

    @Test
    fun `flere varsler enn kun åpen oppgave i gosys`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon = generasjonMedVarsel(1.februar, 28.februar, vedtaksperiodeId, "SB_EX_1")
        assertTrue(generasjon.harKunGosysvarsel())

        generasjon.håndterNyttVarsel(Varsel(UUID.randomUUID(), "RV_MV_1", LocalDateTime.now(), vedtaksperiodeId))
        assertFalse(generasjon.harKunGosysvarsel())
    }

    @Test
    fun `generasjon mangler medlemskapsvarsel`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(vedtaksperiodeId)
        assertFalse(generasjon1.harMedlemskapsvarsel())
    }

    @Test
    fun `haster å behandle hvis generasjonen har varsel om negativt beløp`() {
        val generasjon1 = generasjonMedVarsel(varselkode = "RV_UT_23")
        assertTrue(generasjon1.hasterÅBehandle())
    }

    @Test
    fun `krever skjønnsfastsettelse hvis generasjon har varsel om avvik`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjonMedVarsel(vedtaksperiodeId = vedtaksperiodeId, varselkode = "RV_IV_2")
        assertTrue(generasjon1.kreverSkjønnsfastsettelse())
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
        generasjon1.håndterNyUtbetaling(utbetalingId)
        generasjon1.oppdaterBehandlingsinformasjon(emptyList(), spleisBehandlingId, utbetalingId)
        generasjon1.håndterVedtakFattet()
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId)
        generasjon2.håndterNyUtbetaling(utbetalingId)
        generasjon2.oppdaterBehandlingsinformasjon(emptyList(), spleisBehandlingId, utbetalingId)
        generasjon2.håndterVedtakFattet()
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
        generasjon1.håndterNyUtbetaling(utbetalingId1)
        generasjon1.oppdaterBehandlingsinformasjon(listOf("hei"), spleisBehandlingId, utbetalingId1)
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId)
        val spleisBehandlingId2 = UUID.randomUUID()
        val utbetalingId2 = UUID.randomUUID()
        generasjon2.håndterNyUtbetaling(utbetalingId2)
        generasjon2.oppdaterBehandlingsinformasjon(listOf("hallo"), spleisBehandlingId2, utbetalingId2)
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig utbetalingId`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId)
        generasjon1.håndterNyUtbetaling(UUID.randomUUID())
        val generasjon2 = generasjon(generasjonId, vedtaksperiodeId)
        generasjon2.håndterNyUtbetaling(UUID.randomUUID())
        assertNotEquals(generasjon1, generasjon2)
        assertNotEquals(generasjon1.hashCode(), generasjon2.hashCode())
    }

    @Test
    fun `forskjellig utbetalingId der én generasjon har null`() {
        val generasjonId = UUID.randomUUID()
        val vedtaksperiodeId = UUID.randomUUID()
        val generasjon1 = generasjon(generasjonId, vedtaksperiodeId)
        generasjon1.håndterNyUtbetaling(UUID.randomUUID())
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
        generasjon.håndterNyUtbetaling(utbetalingId)
        val tags = listOf("tag 1")
        generasjon.oppdaterBehandlingsinformasjon(tags, spleisBehandlingId, utbetalingId)

        val varselId = UUID.randomUUID()
        val opprettet = LocalDateTime.now()
        val varsel = Varsel(varselId, "SB_EX_1", opprettet, vedtaksperiodeId, AKTIV)
        generasjon.håndterNyttVarsel(varsel)
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
                null
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
    ): Generasjon =
        generasjon(vedtaksperiodeId = vedtaksperiodeId, fom = fom, tom = tom).also {
            it.håndterNyttVarsel(Varsel(UUID.randomUUID(), varselkode, LocalDateTime.now(), vedtaksperiodeId))
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
    )

    private fun Generasjon.assertVarsler(
        forventetAntall: Int,
        status: VarselStatusDto,
        varselkode: Varselkode,
    ) {
        this.assertVarsler(forventetAntall, status, varselkode.name)
    }

    private fun Generasjon.assertVarsler(
        forventetAntall: Int,
        status: VarselStatusDto,
        varselkode: String,
    ) {
        val dto = this.toDto()
        val varsler = dto.varsler
        val varsel = varsler.filter { it.varselkode == varselkode && it.status == status }
        assertEquals(forventetAntall, varsel.size)
    }

    private fun Generasjon.assertUtbetalingId(utbetalingId: UUID?) {
        val dto = this.toDto()
        assertEquals(utbetalingId, dto.utbetalingId)
    }
}
