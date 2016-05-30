-- Create the table ONLY if not exists
CREATE TABLE IF NOT EXISTS item_coordinates_point (
    item_id INT PRIMARY KEY,
    coordinates  POINT NOT NULL
) ENGINE = MyISAM;

-- Insert into the above table data ONLY if there are no already inside
INSERT IGNORE INTO item_coordinates_point(item_id, coordinates)
    SELECT item_id, POINT(latitude, longitude)
    FROM item_coordinates;

-- Create Spatial Index
-- CREATE SPATIAL INDEX spatialIndex ON item_coordinates_point(coordinates);

-- Create Spatial Index ONLY if spatial Index does not EXISTS

SELECT IF (
    EXISTS(
        SELECT DISTINCT INDEX_NAME  FROM information_schema.statistics
        WHERE table_schema = 'ad'
        AND table_name = 'item_coordinates_point' AND INDEX_NAME  like 'spatialIndex'
    )
    ,'SELECT ''index spatialIndex already exists'' Info;'
    ,'CREATE SPATIAL INDEX spatialIndex on item_coordinates_point(coordinates)') into @a;
PREPARE stmt1 FROM @a;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

