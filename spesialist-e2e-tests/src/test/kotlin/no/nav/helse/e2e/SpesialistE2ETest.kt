package no.nav.helse.e2e

class SpesialistE2ETest {
//    @Test
//    fun `reserverer person ved godkjenning av oppgave`() {
//        saksbehandlerDao.opprettSaksbehandler(
//            GodkjenningE2ETest.SAKSBEHANDLEROID,
//            "Navn Navnesen",
//            GodkjenningE2ETest.SAKSBEHANDLEREPOST,
//            GodkjenningE2ETest.SAKSBEHANDLERIDENT
//        )
//        fremTilSaksbehandleroppgave()
//        håndterSaksbehandlerløsning()
//
//        val saksbehandler = reservasjonDao.hentReservertTil(FØDSELSNUMMER)!!
//
//        assertEquals(GodkjenningE2ETest.SAKSBEHANDLEROID, saksbehandler)
//
//        testRapid.reset()
//
//        val VEDTAKSPERIODE_ID2 = UUID.randomUUID()
//        val UTBETALING_ID2 = UUID.randomUUID()
//        sendVedtaksperiodeOpprettet(
//            AKTØR,
//            FØDSELSNUMMER,
//            ORGNR,
//            vedtaksperiodeId = VEDTAKSPERIODE_ID2,
//            skjæringstidspunkt = 1.januar,
//            fom = 1.januar,
//            tom = 31.januar
//        )
//        sendVedtaksperiodeNyUtbetaling(VEDTAKSPERIODE_ID2, utbetalingId = UTBETALING_ID2, organisasjonsnummer = ORGNR)
//        sendUtbetalingEndret(AKTØR, FØDSELSNUMMER, ORGNR, UTBETALING_ID2, "UTBETALING")
//        every { snapshotClient.hentSnapshot(FØDSELSNUMMER) } returns snapshotMedWarnings(
//            vedtaksperiodeId = VEDTAKSPERIODE_ID2,
//            orgnr = ORGNR,
//            fnr = FØDSELSNUMMER,
//            aktørId = AKTØR,
//            utbetalingId = UTBETALING_ID2,
//        )
//        sendAktivitetsloggNyAktivitet(AKTØR, FØDSELSNUMMER, ORGNR, VEDTAKSPERIODE_ID2, listOf("SB_EX_1"))
//        val godkjenningsmeldingId2 = sendGodkjenningsbehov(
//            organisasjonsnummer = ORGNR,
//            vedtaksperiodeId = VEDTAKSPERIODE_ID2,
//            utbetalingId = UTBETALING_ID2,
//            skjæringstidspunkt = 1.januar,
//            periodeFom = 1.januar,
//            periodeTom = 31.januar
//        )
//        sendPersoninfoløsningComposite(godkjenningsmeldingId2, ORGNR, VEDTAKSPERIODE_ID2)
//        sendEgenAnsattløsningOld(
//            godkjenningsmeldingId = godkjenningsmeldingId2,
//            erEgenAnsatt = false
//        )
//        sendVergemålløsningOld(godkjenningsmeldingId2)
//        sendÅpneGosysOppgaverløsningOld(
//            godkjenningsmeldingId = godkjenningsmeldingId2,
//        )
//        sendRisikovurderingløsningOld(
//            godkjenningsmeldingId = godkjenningsmeldingId2,
//            vedtaksperiodeId = VEDTAKSPERIODE_ID2,
//        )
//
//        val tildeling = tildelingDao.tildelingForOppgave(OPPGAVEID)
//        assertEquals(GodkjenningE2ETest.SAKSBEHANDLEREPOST, tildeling?.epost)
//    }

}