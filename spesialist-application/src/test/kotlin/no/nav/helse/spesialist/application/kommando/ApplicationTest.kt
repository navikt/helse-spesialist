package no.nav.helse.spesialist.application.kommando

import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.spesialist.application.InMemoryMeldingPubliserer
import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.Testdata
import no.nav.helse.spesialist.domain.Behandling
import no.nav.helse.spesialist.domain.Varsel
import no.nav.helse.spesialist.domain.VarselId
import no.nav.helse.spesialist.domain.Varselvurdering
import no.nav.helse.spesialist.domain.testfixtures.feb
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagBehandling
import no.nav.helse.spesialist.domain.testfixtures.lagVarseldefinisjonId
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagPerson
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertNull
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertNotNull

abstract class ApplicationTest {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    val sessionContext = inMemoryRepositoriesAndDaos.sessionContext
    val outbox = Outbox("1.0.0")

    val person =
        lagPerson().also {
            sessionContext.personRepository.lagre(it)
        }

    private val skjæringstidspunkt = 1.jan(2018)
    val vedtaksperiode1 = lagVedtaksperiode(identitetsnummer = person.id).also { sessionContext.vedtaksperiodeRepository.lagre(it) }
    val vedtaksperiode2 = lagVedtaksperiode(identitetsnummer = person.id).also { sessionContext.vedtaksperiodeRepository.lagre(it) }
    val behandling1 = lagBehandling(vedtaksperiodeId = vedtaksperiode1.id, fom = 1.jan(2018), tom = 31.jan(2018), skjæringstidspunkt = skjæringstidspunkt).also { sessionContext.behandlingRepository.lagre(it) }
    val behandling2 = lagBehandling(vedtaksperiodeId = vedtaksperiode2.id, fom = 1.feb(2018), tom = 28.feb(2018), skjæringstidspunkt = skjæringstidspunkt).also { sessionContext.behandlingRepository.lagre(it) }

    val godkjenningsbehovData =
        Testdata.godkjenningsbehovData(
            fødselsnummer = person.id.value,
            vedtaksperiodeId = vedtaksperiode1.id.value,
            spleisBehandlingId = behandling1.spleisBehandlingId!!.value,
            skjæringstidspunkt = skjæringstidspunkt,
        )

    inline fun <reified T : UtgåendeHendelse> assertUtgåendeHendelse(assertBlock: (T) -> Unit = {}) {
        val meldingPubliserer = InMemoryMeldingPubliserer()
        outbox.sendAlle(meldingPubliserer)
        val melding =
            meldingPubliserer.publiserteUtgåendeHendelser
                .map { it.hendelse }
                .filterIsInstance<T>()
                .singleOrNull()
        assertNotNull(melding)
        assertBlock(melding)
    }

    protected fun Behandling.nyttVarsel(
        varselkode: String,
        status: Varsel.Status = Varsel.Status.AKTIV,
    ) {
        val varsel =
            Varsel.fraLagring(
                id = VarselId(UUID.randomUUID()),
                behandlingUnikId = this.id,
                spleisBehandlingId = this.spleisBehandlingId,
                kode = varselkode,
                opprettetTidspunkt = LocalDateTime.now(),
                status = status,
                vurdering =
                    if (status == Varsel.Status.AKTIV) {
                        null
                    } else {
                        Varselvurdering(
                            saksbehandlerId = lagSaksbehandler().id,
                            tidspunkt = LocalDateTime.now(),
                            vurdertDefinisjonId = lagVarseldefinisjonId(),
                        )
                    },
            )
        sessionContext.varselRepository.lagre(varsel)
    }

    protected fun Behandling.assertAntallVarsler(forventetAntall: Int) {
        val antallVarsler = sessionContext.varselRepository.finnVarslerFor(this.id).size
        assertEquals(forventetAntall, antallVarsler)
    }

    protected fun Behandling.assertHarVarsel(
        forventetKode: String,
        forventetStatus: Varsel.Status,
    ) {
        val varsel = sessionContext.varselRepository.finnVarslerFor(this.id).find { it.kode == forventetKode }
        assertNotNull(varsel, "Forventet å finne varsel med kode $forventetKode, men fant ingen")
        assertEquals(forventetStatus, varsel.status)
    }

    protected fun Behandling.assertHarIkkeVarsel(
        forventetKode: String,
    ) {
        val varsel = sessionContext.varselRepository.finnVarslerFor(this.id).find { it.kode == forventetKode }
        assertNull(varsel, "Forventet å ikke finne varsel med kode $forventetKode, men fant en")
    }

    inline fun <reified T : UtgåendeHendelse> assertIkkeUtgåendeHendelse() {
        val meldingPubliserer = InMemoryMeldingPubliserer()
        outbox.sendAlle(meldingPubliserer)
        val melding =
            meldingPubliserer.publiserteUtgåendeHendelser
                .map { it.hendelse }
                .filterIsInstance<T>()
                .singleOrNull()
        assertNull(melding)
    }
}
