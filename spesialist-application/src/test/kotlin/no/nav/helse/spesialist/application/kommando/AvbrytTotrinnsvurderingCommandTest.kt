package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.kommando.AvbrytTotrinnsvurderingCommand
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.Periode.Companion.tilOgMed
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Totrinnsvurdering
import no.nav.helse.spesialist.domain.TotrinnsvurderingId
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.saksbehandlerhandling.HandlingType
import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling
import no.nav.helse.spesialist.domain.saksbehandlerhandling.SaksbehandlerhandlingId
import no.nav.helse.spesialist.domain.tilkommeninntekt.TilkommenInntekt
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagSkjønnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.mar
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AvbrytTotrinnsvurderingCommandTest : ApplicationTest() {
    private val identitetsnummer = lagPerson().also(sessionContext.personRepository::lagre).id

    @Test
    fun `forkaster totrinnsvurdering når alle vedtaksperioder med overstyring er forkastet`() {
        val periode = lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering(overstyringFor = listOf(periode))

        utfør()

        assertTrue(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `beholder totrinnsvurdering når en vedtaksperiode med overstyring er aktiv`() {
        val forkastet = lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        val aktiv = lagPeriode(skjæringstidspunkt = 1.jan(2018))
        val totrinnsvurdering = lagTotrinnsvurdering(overstyringFor = listOf(forkastet, aktiv))

        utfør()

        assertFalse(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `beholder totrinnsvurdering når handling med skjæringstidspunkt har aktiv periode på skjæringstidspunktet`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.jan(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagHandling(totrinnsvurdering.id(), skjæringstidspunkt = 1.jan(2018))

        utfør()

        assertFalse(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `forkaster totrinnsvurdering når alle perioder på skjæringstidspunktet til handlingen er forkastet`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagHandling(totrinnsvurdering.id(), skjæringstidspunkt = 1.jan(2018))

        utfør()

        assertTrue(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `forkaster totrinnsvurdering når perioden til handlingens behandlingId er forkastet, selv om andre perioder er aktive`() {
        val forkastet = lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.jan(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagHandling(totrinnsvurdering.id(), behandlingId = forkastet.second)

        utfør()

        assertTrue(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `beholder totrinnsvurdering når perioden til handlingens behandlingId er aktiv`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        val aktiv = lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagHandling(totrinnsvurdering.id(), behandlingId = aktiv.second)

        utfør()

        assertFalse(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `behandlingId går foran skjæringstidspunkt`() {
        val forkastet = lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.jan(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagHandling(totrinnsvurdering.id(), behandlingId = forkastet.second, skjæringstidspunkt = 1.jan(2018))

        utfør()

        assertTrue(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `beholder totrinnsvurdering når handling uten skjæringstidspunkt og behandlingId og personen har aktiv periode`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagHandling(totrinnsvurdering.id())

        utfør()

        assertFalse(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `forkaster totrinnsvurdering når handling uten skjæringstidspunkt og behandlingId og personen ikke har aktive perioder`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagHandling(totrinnsvurdering.id())

        utfør()

        assertTrue(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `beholder totrinnsvurdering når overstyrt periode er forkastet, men handling holder den i live`() {
        val forkastet = lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering(overstyringFor = listOf(forkastet))
        lagHandling(totrinnsvurdering.id(), skjæringstidspunkt = 1.mar(2018))

        utfør()

        assertFalse(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `ignorerer handlinger som ikke peker på totrinnsvurderingen`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagHandling(totrinnsvurderingId = null)
        lagHandling(totrinnsvurderingId = TotrinnsvurderingId(totrinnsvurdering.id().value + 1000))

        utfør()

        assertTrue(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `beholder totrinnsvurdering når tilkommen inntekt overlapper aktiv periode`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagTilkommenInntekt(totrinnsvurdering.id(), periode = 5.mar(2018) tilOgMed 10.mar(2018))

        utfør()

        assertFalse(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `forkaster totrinnsvurdering når alle perioder som overlapper tilkommen inntekt er forkastet`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagTilkommenInntekt(totrinnsvurdering.id(), periode = 5.jan(2018) tilOgMed 10.jan(2018))

        utfør()

        assertTrue(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `beholder totrinnsvurdering når overstyrt periode er forkastet, men tilkommen inntekt overlapper aktiv periode`() {
        val forkastet = lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering(overstyringFor = listOf(forkastet))
        lagTilkommenInntekt(totrinnsvurdering.id(), periode = 5.mar(2018) tilOgMed 10.mar(2018))

        utfør()

        assertFalse(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `ignorerer fjernet tilkommen inntekt`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagTilkommenInntekt(totrinnsvurdering.id(), periode = 5.mar(2018) tilOgMed 10.mar(2018), fjernet = true)

        utfør()

        assertTrue(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `ignorerer tilkommen inntekt som peker på en annen totrinnsvurdering`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagTilkommenInntekt(
            TotrinnsvurderingId(totrinnsvurdering.id().value + 1000),
            periode = 5.mar(2018) tilOgMed 10.mar(2018),
        )

        utfør()

        assertTrue(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    @Test
    fun `tilkommen inntekt som overlapper aktiv periode med én dag holder totrinnsvurderingen i live`() {
        lagPeriode(skjæringstidspunkt = 1.jan(2018), forkastet = true)
        lagPeriode(skjæringstidspunkt = 1.mar(2018))
        val totrinnsvurdering = lagTotrinnsvurdering()
        lagTilkommenInntekt(totrinnsvurdering.id(), periode = 20.feb(2018) tilOgMed 1.mar(2018))

        utfør()

        assertFalse(hent(totrinnsvurdering).vedtaksperiodeForkastet)
    }

    private fun utfør() {
        assertTrue(
            AvbrytTotrinnsvurderingCommand(identitetsnummer).execute(CommandContext(UUID.randomUUID()), sessionContext, outbox),
        )
    }

    private fun hent(totrinnsvurdering: Totrinnsvurdering): Totrinnsvurdering =
        sessionContext.totrinnsvurderingRepository.finnOrNull(totrinnsvurdering.id())!!

    private fun lagPeriode(
        skjæringstidspunkt: LocalDate,
        forkastet: Boolean = false,
    ): Pair<Vedtaksperiode, SpleisBehandlingId> {
        val vedtaksperiode =
            lagVedtaksperiode(identitetsnummer = identitetsnummer, forkastet = forkastet)
                .also(sessionContext.vedtaksperiodeRepository::lagre)
        val behandling =
            lagBehandling(
                vedtaksperiodeId = vedtaksperiode.id,
                fom = skjæringstidspunkt,
                tom = skjæringstidspunkt.plusDays(27),
                skjæringstidspunkt = skjæringstidspunkt,
            ).also(sessionContext.behandlingRepository::lagre)
        return vedtaksperiode to behandling.spleisBehandlingId!!
    }

    private fun lagTotrinnsvurdering(overstyringFor: List<Pair<Vedtaksperiode, SpleisBehandlingId>> = emptyList()): Totrinnsvurdering =
        Totrinnsvurdering.ny(identitetsnummer.value).also { totrinnsvurdering ->
            overstyringFor.forEach { (vedtaksperiode, _) ->
                totrinnsvurdering.nyOverstyring(
                    lagSkjønnsfastsattSykepengegrunnlag(
                        saksbehandlerOid = lagSaksbehandler().id,
                        vedtaksperiodeId = vedtaksperiode.id,
                        identitetsnummer = identitetsnummer,
                        aktørId = "1234567891011",
                        skjæringstidspunkt = 1.feb(2018),
                        organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                        skjønnsfastsattBeløp = BigDecimal("500000"),
                        omregnetÅrsinntekt = BigDecimal("400000"),
                    ),
                )
            }
            sessionContext.totrinnsvurderingRepository.lagre(totrinnsvurdering)
        }

    private fun lagTilkommenInntekt(
        totrinnsvurderingId: TotrinnsvurderingId,
        periode: Periode,
        fjernet: Boolean = false,
    ) {
        val tilkommenInntekt =
            TilkommenInntekt.ny(
                identitetsnummer = identitetsnummer,
                saksbehandlerIdent = NAVIdent("A123456"),
                notatTilBeslutter = "notat",
                totrinnsvurderingId = totrinnsvurderingId,
                organisasjonsnummer = "999999999",
                periode = periode,
                periodebeløp = BigDecimal("10000"),
                ekskluderteUkedager = emptySet(),
            )
        if (fjernet) {
            tilkommenInntekt.fjern(
                saksbehandlerIdent = NAVIdent("A123456"),
                notatTilBeslutter = "fjernet",
                totrinnsvurderingId = totrinnsvurderingId,
            )
        }
        sessionContext.tilkommenInntektRepository.lagre(tilkommenInntekt)
    }

    private fun lagHandling(
        totrinnsvurderingId: TotrinnsvurderingId?,
        behandlingId: SpleisBehandlingId? = null,
        skjæringstidspunkt: LocalDate? = null,
    ) {
        sessionContext.saksbehandlerhandlingRepository.lagre(
            Saksbehandlerhandling.ny(
                id = SaksbehandlerhandlingId(UUID.randomUUID()),
                identitetsnummer = identitetsnummer,
                handlingId = UUID.randomUUID(),
                handlingType = HandlingType.MANUELT_VURDERT_OPPTJENING,
                saksbehandlerIdent = "A123456",
                tidspunkt = Instant.now(),
                skjæringstidspunkt = skjæringstidspunkt,
                behandlingId = behandlingId,
                totrinnsvurderingId = totrinnsvurderingId,
            ),
        )
    }
}
