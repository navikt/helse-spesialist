UPDATE selve_varsel SET generasjon_ref = 2129490 WHERE generasjon_ref = 20463;
UPDATE selve_varsel SET generasjon_ref = 241498 WHERE generasjon_ref = 31337;
UPDATE selve_varsel SET generasjon_ref = 4476796 WHERE generasjon_ref = 50153;
UPDATE selve_varsel SET generasjon_ref = 847922 WHERE generasjon_ref = 847920;
UPDATE selve_varsel SET generasjon_ref = 2489071 WHERE generasjon_ref = 2489069;
UPDATE selve_varsel SET generasjon_ref = 3804686 WHERE generasjon_ref = 3804684;

DELETE FROM selve_vedtaksperiode_generasjon
WHERE unik_id IN (
                  'e5111b29-f2b8-4ed2-9deb-28c19a0a7bd1',
                  '053c2a9b-45e7-4a86-8e58-278488f9499a',
                  'bbb13ea1-37f1-4eea-b29b-4fc682c7fee3',
                  '9a8b4431-f9f9-4d9a-b8b5-b5e46a15f480',
                  'b2a3564f-4248-407c-9ce1-c9ae68ba9f0e',
                  '527e0b3c-9442-4fb2-adf2-a3d92b1145d1',
                  '08b2419f-d6a7-4a93-938d-96bdc1633900',
                  'bb1da1f9-dce8-45c4-b6c4-6a4f62631dba',
                  '8b5cba7a-3538-4421-a8f5-231d07c6bbb7',
                  '3fdfb3f9-3dab-4f22-a8e2-39ff794f06a3',
                  'c02494ec-6fb1-4885-8339-6ba5e11d3b75',
                  'fd91f7b8-10e6-4a11-a5b7-a8e2867eaf74',
                  'b7104ff4-5746-4b37-9d3f-43de6d051ca9',
                  'e834736e-092e-40be-9aee-94fd6d37800d',
                  '438a30ee-1ffb-4533-a529-03d8be128b6d',
                  '5bf29f59-e99b-46ae-a939-62d0f88b3529',
                  '4e059c6c-7bfb-42d4-837a-3d7913baf924',
                  'f86bc9a2-237b-407c-a49d-7a7a59bf0cf7',
                  '493edfcd-b5a2-47b2-b169-56c808655f6b',
                  '897602a3-b309-4794-aca4-7af89ac180b4',
                  'e30178cb-78f2-40da-9c80-014571f2f9a7',
                  '65d85557-426a-4d29-b02d-fa903a8a6441',
                  'd003435f-63e3-4175-823c-1633f2845e25',
                  '59d7a1fa-69f2-455a-b6bc-ee823f300f93'
    );