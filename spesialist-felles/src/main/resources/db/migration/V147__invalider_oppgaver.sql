/*
ID-er hentet med:

SELECT *
FROM oppgave o1
WHERE (SELECT COUNT(*)
       FROM oppgave o2
       WHERE o1.vedtak_ref = o2.vedtak_ref
         AND status = 'Ferdigstilt'
         AND o2.opprettet > '2021-02-01 00:00:00'
      ) > 0
  AND o1.status = 'AvventerSaksbehandler'
  AND o1.opprettet > '2021-02-01 00:00:00';
*/

UPDATE oppgave
SET status='Invalidert'::oppgavestatus
WHERE id IN (
             2399207,
             2411922,
             2411919,
             2411918,
             2411920,
             2411934,
             2411923,
             2411940,
             2411929,
             2411935,
             2411938,
             2411943,
             2411944,
             2411955,
             2411956,
             2411945,
             2411946,
             2411950,
             2411954,
             2411960,
             2411962,
             2411963,
             2411975,
             2411966,
             2411967,
             2411968,
             2411969,
             2411971,
             2411973,
             2411974,
             2411979,
             2411986,
             2411981,
             2411989,
             2411983,
             2411990,
             2411991,
             2412001,
             2411999,
             2412000,
             2412005,
             2412011,
             2412013,
             2412015,
             2412017,
             2412018,
             2412019,
             2412021,
             2412022,
             2412027,
             2412025,
             2412029,
             2412028,
             2412035,
             2412048,
             2412046,
             2412054,
             2412041,
             2412047,
             2412044,
             2412051,
             2412058,
             2412074,
             2412063,
             2412073,
             2412075,
             2412082,
             2412083,
             2412090,
             2412092,
             2412089,
             2412093,
             2412094,
             2412097,
             2412096,
             2412101,
             2412102,
             2412106,
             2412126,
             2412116,
             2412119,
             2412122,
             2412124,
             2412132,
             2412142,
             2412133,
             2412135,
             2412138,
             2412144,
             2412145,
             2412137,
             2412146,
             2412148,
             2412149,
             2412153,
             2412154,
             2412155,
             2412156,
             2412160,
             2412165,
             2412161,
             2412167,
             2412164,
             2412166,
             2412170,
             2412171,
             2412173,
             2412175,
             2412176,
             2412704,
             2401860)
