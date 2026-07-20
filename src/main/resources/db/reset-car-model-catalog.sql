/* Reset vehicle catalog to exactly 7 brands x 10 models.
   Existing users, products, services and orders are preserved.
   Existing product compatibility links and vehicle catalog references are reset. */
USE CarShowRoom;
GO

SET NOCOUNT ON;

-- Remove references before replacing the catalog.
DELETE FROM product_car_models;
UPDATE vehicle SET car_model_id = NULL WHERE car_model_id IS NOT NULL;
DELETE FROM car_model;

DECLARE @models TABLE (
    brand NVARCHAR(100) NOT NULL,
    model_name NVARCHAR(100) NOT NULL,
    [year] INT NOT NULL
);

INSERT INTO @models (brand, model_name, [year]) VALUES
-- Honda
(N'Honda', N'City', 2024),
(N'Honda', N'Civic', 2024),
(N'Honda', N'Accord', 2024),
(N'Honda', N'CR-V', 2024),
(N'Honda', N'HR-V', 2024),
(N'Honda', N'BR-V', 2024),
(N'Honda', N'Jazz', 2021),
(N'Honda', N'Brio', 2021),
(N'Honda', N'Mobilio', 2021),
(N'Honda', N'Odyssey', 2023),
-- BMW
(N'BMW', N'3 Series', 2024),
(N'BMW', N'5 Series', 2024),
(N'BMW', N'7 Series', 2024),
(N'BMW', N'X1', 2024),
(N'BMW', N'X3', 2024),
(N'BMW', N'X5', 2024),
(N'BMW', N'X6', 2024),
(N'BMW', N'X7', 2024),
(N'BMW', N'i4', 2024),
(N'BMW', N'iX', 2024),
-- Mercedes-Benz
(N'Mercedes-Benz', N'C-Class', 2024),
(N'Mercedes-Benz', N'E-Class', 2024),
(N'Mercedes-Benz', N'S-Class', 2024),
(N'Mercedes-Benz', N'A-Class', 2023),
(N'Mercedes-Benz', N'GLC', 2024),
(N'Mercedes-Benz', N'GLE', 2024),
(N'Mercedes-Benz', N'GLS', 2024),
(N'Mercedes-Benz', N'GLA', 2024),
(N'Mercedes-Benz', N'EQE', 2024),
(N'Mercedes-Benz', N'EQS', 2024),
-- Toyota
(N'Toyota', N'Vios', 2024),
(N'Toyota', N'Camry', 2024),
(N'Toyota', N'Corolla Cross', 2024),
(N'Toyota', N'Corolla Altis', 2024),
(N'Toyota', N'Fortuner', 2024),
(N'Toyota', N'Innova', 2024),
(N'Toyota', N'Hilux', 2024),
(N'Toyota', N'Yaris', 2023),
(N'Toyota', N'Raize', 2024),
(N'Toyota', N'Alphard', 2024),
-- Mitsubishi
(N'Mitsubishi', N'Xpander', 2024),
(N'Mitsubishi', N'Outlander', 2024),
(N'Mitsubishi', N'Pajero Sport', 2024),
(N'Mitsubishi', N'Triton', 2024),
(N'Mitsubishi', N'Attrage', 2023),
(N'Mitsubishi', N'Mirage', 2021),
(N'Mitsubishi', N'ASX', 2023),
(N'Mitsubishi', N'Eclipse Cross', 2024),
(N'Mitsubishi', N'Delica', 2023),
(N'Mitsubishi', N'Lancer', 2017),
-- VinFast
(N'VinFast', N'Fadil', 2022),
(N'VinFast', N'Lux A2.0', 2022),
(N'VinFast', N'Lux SA2.0', 2022),
(N'VinFast', N'VF e34', 2024),
(N'VinFast', N'VF 5', 2024),
(N'VinFast', N'VF 6', 2024),
(N'VinFast', N'VF 7', 2024),
(N'VinFast', N'VF 8', 2024),
(N'VinFast', N'VF 9', 2024),
(N'VinFast', N'President', 2022),
-- KIA
(N'KIA', N'Morning', 2024),
(N'KIA', N'Soluto', 2024),
(N'KIA', N'Cerato', 2023),
(N'KIA', N'K3', 2024),
(N'KIA', N'K5', 2024),
(N'KIA', N'Seltos', 2024),
(N'KIA', N'Sonet', 2024),
(N'KIA', N'Sportage', 2024),
(N'KIA', N'Sorento', 2024),
(N'KIA', N'Carnival', 2024);

INSERT INTO car_model (brand, model_name, [year])
SELECT brand, model_name, [year] FROM @models;

SELECT brand, COUNT(*) AS model_count
FROM car_model
GROUP BY brand
ORDER BY brand;
GO
