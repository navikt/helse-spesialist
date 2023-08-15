package no.nav.helse.db

import DatabaseIntegrationTest
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.modell.overstyring.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.sykefraværstilfelle.SkjønnsfastattSykepengegrunnlag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykefraværstilfelleDaoTest: DatabaseIntegrationTest() {
    private val sykefraværstilfelleDao = SykefraværstilfelleDao(dataSource)

    @Test
    fun `Finn skjønnsfastsatt sykepengegrunnlag`() {
        nyPerson()
        opprettSaksbehandler()
        val hendelseId = UUID.randomUUID()
        testhendelse(hendelseId)
        val tidspunkt = LocalDateTime.now()
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            hendelseId = hendelseId,
            eksternHendelseId = UUID.randomUUID(),
            fødselsnummer = FNR,
            arbeidsgivere = listOf(
                SkjønnsfastsattArbeidsgiver(ORGNUMMER, 1000.0, 900.0, "En årsak", "En ", "begrunnelse", null, null)
            ),
            saksbehandlerRef = SAKSBEHANDLER_OID,
            skjæringstidspunkt = 1.januar,
            tidspunkt = tidspunkt
        )

        val funnet = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(FNR, 1.januar)
        assertEquals(1, funnet.size)
        assertEquals(SkjønnsfastattSykepengegrunnlag(1.januar, "En ", "begrunnelse", tidspunkt), funnet.single())
    }

    @Test
    fun `Finn kun skjønnsfastsatt sykepengegrunnlag for bestemt fnr`() {
        nyPerson()
        val person2 = "111111111111"
        val arbeidsgiver2 = "999999999"
        nyPerson(fødselsnummer = person2, organisasjonsnummer = arbeidsgiver2, vedtaksperiodeId = UUID.randomUUID())
        opprettSaksbehandler()
        val hendelseId1 = UUID.randomUUID()
        val hendelseId2 = UUID.randomUUID()
        testhendelse(hendelseId1)
        testhendelse(hendelseId2)
        val tidspunkt = LocalDateTime.now()
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            hendelseId = hendelseId1,
            eksternHendelseId = UUID.randomUUID(),
            fødselsnummer = FNR,
            arbeidsgivere = listOf(
                SkjønnsfastsattArbeidsgiver(ORGNUMMER, 1000.0, 900.0, "En årsak", "En ", "begrunnelse", null, null)
            ),
            saksbehandlerRef = SAKSBEHANDLER_OID,
            skjæringstidspunkt = 1.januar,
            tidspunkt = tidspunkt
        )
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            hendelseId = hendelseId2,
            eksternHendelseId = UUID.randomUUID(),
            fødselsnummer = person2,
            arbeidsgivere = listOf(
                SkjønnsfastsattArbeidsgiver(arbeidsgiver2, 1000.0, 900.0, "En årsak", "En ", "begrunnelse", null, null)
            ),
            saksbehandlerRef = SAKSBEHANDLER_OID,
            skjæringstidspunkt = 1.januar,
            tidspunkt = LocalDateTime.now()
        )

        val funnet = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(FNR, 1.januar)
        assertEquals(1, funnet.size)
        assertEquals(SkjønnsfastattSykepengegrunnlag(1.januar, "En ", "begrunnelse", tidspunkt), funnet.single())
    }

    @Test
    fun `Finner skjønnsfastsatt sykepengegrunnlag kun for aktuelt skjæringstidspunkt`() {
        nyPerson()
        opprettSaksbehandler()
        val hendelseId1 = UUID.randomUUID()
        val hendelseId2 = UUID.randomUUID()
        testhendelse(hendelseId1)
        testhendelse(hendelseId2)
        val tidspunkt = LocalDateTime.now()
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            hendelseId = hendelseId1,
            eksternHendelseId = UUID.randomUUID(),
            fødselsnummer = FNR,
            arbeidsgivere = listOf(
                SkjønnsfastsattArbeidsgiver(ORGNUMMER, 1000.0, 900.0, "En årsak", "En ", "begrunnelse", null, null)
            ),
            saksbehandlerRef = SAKSBEHANDLER_OID,
            skjæringstidspunkt = 1.januar,
            tidspunkt = tidspunkt
        )
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            hendelseId = hendelseId2,
            eksternHendelseId = UUID.randomUUID(),
            fødselsnummer = FNR,
            arbeidsgivere = listOf(
                SkjønnsfastsattArbeidsgiver(ORGNUMMER, 1000.0, 900.0, "En årsak", "En ", "begrunnelse", null, null)
            ),
            saksbehandlerRef = SAKSBEHANDLER_OID,
            skjæringstidspunkt = 1.februar,
            tidspunkt = LocalDateTime.now()
        )

        val funnet = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(FNR, 1.januar)
        assertEquals(1, funnet.size)
        assertEquals(SkjønnsfastattSykepengegrunnlag(1.januar, "En ", "begrunnelse", tidspunkt), funnet.single())
    }

}