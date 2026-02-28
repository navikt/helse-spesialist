-- Ferdigstill totrinnsvurdering som feilaktig hadde blitt endret
UPDATE totrinnsvurdering SET
     utbetaling_id_ref = (SELECT id FROM utbetaling_id WHERE utbetaling_id = 'b0244bf6-ece6-4178-a87f-4568138a2ee0'),
     beslutter = '3cac4e6a-56d0-4c9a-a539-7fe674f25c4e',
     oppdatert = '2023-08-25T11:05:32.601927402'
WHERE id = 5540;

UPDATE totrinnsvurdering SET
     utbetaling_id_ref = (SELECT id FROM utbetaling_id WHERE utbetaling_id = '8859be6e-e097-4b09-a5c3-b370a45cfd2d'),
     beslutter = 'aad83238-3f5c-43be-a296-63b3d8479ebc',
     oppdatert = '2023-09-06T10:38:47.516150713'
WHERE id = 6595;

UPDATE totrinnsvurdering SET
     utbetaling_id_ref = (SELECT id FROM utbetaling_id WHERE utbetaling_id = '2c82b845-eddd-4d0e-8750-5b6b2d824c20'),
     beslutter = '73f22b3a-06c6-4373-84f1-ba54ca66d137',
     oppdatert = '2023-09-12T10:11:51.493273342'
WHERE id = 7147;