
-- DROP INDEX ONLY IF IT EXISTS!
SELECT IF (
    NOT EXISTS(
        SELECT DISTINCT INDEX_NAME  FROM information_schema.statistics
        WHERE table_schema = 'ad'
        AND table_name = 'item_coordinates_point' AND INDEX_NAME  like 'spatialIndex'
    )
    ,'SELECT ''index spatialIndex does not Exist'' Info;'
    ,'DROP INDEX spatialIndex ON item_coordinates_point') into @a;
PREPARE stmt1 FROM @a;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- DROP TABLE ONLY IF IT EXISTS
DROP TABLE IF EXISTS 
ad.item_coordinates_point;


