UPDATE api_varseldefinisjon
SET kode = CASE kode
               WHEN 'SB_RV_1' THEN (SELECT kode FROM api_varseldefinisjon WHERE kode = 'SB_RV_2' LIMIT 1)
               WHEN 'SB_RV_2' THEN (SELECT kode FROM api_varseldefinisjon WHERE kode = 'SB_RV_1' LIMIT 1)
    END
WHERE kode IN ('SB_RV_1','SB_RV_2');