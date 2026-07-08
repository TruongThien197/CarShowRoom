/* GearShift Pro catalog car model seed data.
   Run this manually in SQL Server when you want to load vehicle catalog data.
   This script inserts into car_model and product_car_models only.
   It does not insert or update customer vehicles in the vehicle table. */

SET NOCOUNT ON;

DECLARE @car_models TABLE (
    brand NVARCHAR(100) NOT NULL,
    model_name NVARCHAR(100) NOT NULL,
    [year] INT NOT NULL
);

INSERT INTO @car_models (brand, model_name, [year]) VALUES
(N'BMW', N'114i', 2023),
(N'BMW', N'116i', 2023),
(N'BMW', N'118i', 2023),
(N'BMW', N'120d', 2023),
(N'BMW', N'120i', 2023),
(N'BMW', N'128i', 2023),
(N'BMW', N'135is', 2023),
(N'BMW', N'218i', 2023),
(N'BMW', N'316d', 2023),
(N'BMW', N'316i', 2023),
(N'BMW', N'318ci', 2023),
(N'BMW', N'318d', 2023),
(N'BMW', N'318i', 2023),
(N'BMW', N'318i/320i', 2023),
(N'BMW', N'318is', 2023),
(N'BMW', N'320d', 2023),
(N'BMW', N'320i', 2023),
(N'BMW', N'325ci', 2023),
(N'BMW', N'325i', 2023),
(N'BMW', N'325i ul', 2023),
(N'BMW', N'328i', 2023),
(N'BMW', N'328xi', 2023),
(N'BMW', N'330ci', 2023),
(N'BMW', N'330i', 2023),
(N'BMW', N'330xi', 2023),
(N'BMW', N'335i', 2023),
(N'BMW', N'335is', 2023),
(N'BMW', N'335Xi', 2023),
(N'BMW', N'420i', 2023),
(N'BMW', N'428i', 2023),
(N'BMW', N'430i', 2023),
(N'BMW', N'440i', 2023),
(N'BMW', N'520d', 2023),
(N'BMW', N'520i', 2023),
(N'BMW', N'523i', 2023),
(N'BMW', N'525d', 2023),
(N'BMW', N'525i', 2023),
(N'BMW', N'525xi', 2023),
(N'BMW', N'528i', 2023),
(N'BMW', N'528xi', 2023),
(N'BMW', N'530d', 2023),
(N'BMW', N'530dx', 2023),
(N'BMW', N'530i', 2023),
(N'BMW', N'530xi', 2023),
(N'BMW', N'535d', 2023),
(N'BMW', N'535i', 2023),
(N'BMW', N'535ix', 2023),
(N'BMW', N'540i', 2023),
(N'BMW', N'545i', 2023),
(N'BMW', N'550i', 2023),
(N'BMW', N'M3', 2020),
(N'BMW', N'M4 G82', 2024),
(N'BMW', N'X5', 2022),
(N'Toyota', N'Camry', 2022),
(N'Toyota', N'Corolla Cross', 2023),
(N'Toyota', N'Fortuner', 2021),
(N'Toyota', N'Supra GR', 2022),
(N'Honda', N'Civic', 2022),
(N'Honda', N'Accord', 2021),
(N'Honda', N'CR-V', 2023),
(N'Mazda', N'Mazda 3', 2022),
(N'Mazda', N'Mazda 6', 2021),
(N'Mazda', N'CX-5', 2023),
(N'Mercedes-Benz', N'C200', 2022),
(N'Mercedes-Benz', N'E300', 2021),
(N'Mercedes-Benz', N'GLC 300', 2023),
(N'Porsche', N'911 Carrera', 2023),
(N'Porsche', N'Cayenne', 2022),
(N'Porsche', N'Macan', 2021);

INSERT INTO car_model (brand, model_name, [year])
SELECT src.brand, src.model_name, src.[year]
FROM @car_models src
WHERE NOT EXISTS (
    SELECT 1
    FROM car_model cm
    WHERE LOWER(cm.brand) = LOWER(src.brand)
      AND LOWER(cm.model_name) = LOWER(src.model_name)
      AND cm.[year] = src.[year]
);

/* Demo product compatibility mapping.
   BMW applies to all seeded products.
   Toyota/Honda/Mazda apply to Brakes and Oil & Fluids.
   Mercedes-Benz/Porsche apply to Engine Parts and Suspension. */

INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.id, cm.car_model_id
FROM products p
CROSS JOIN car_model cm
WHERE cm.brand = N'BMW'
  AND NOT EXISTS (
      SELECT 1
      FROM product_car_models pcm
      WHERE pcm.product_id = p.id
        AND pcm.car_model_id = cm.car_model_id
  );

INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.id, cm.car_model_id
FROM products p
JOIN category c ON c.category_id = p.category_id
CROSS JOIN car_model cm
WHERE c.category_name IN (N'Brakes', N'Oil & Fluids')
  AND cm.brand IN (N'Toyota', N'Honda', N'Mazda')
  AND NOT EXISTS (
      SELECT 1
      FROM product_car_models pcm
      WHERE pcm.product_id = p.id
        AND pcm.car_model_id = cm.car_model_id
  );

INSERT INTO product_car_models (product_id, car_model_id)
SELECT p.id, cm.car_model_id
FROM products p
JOIN category c ON c.category_id = p.category_id
CROSS JOIN car_model cm
WHERE c.category_name IN (N'Engine Parts', N'Suspension')
  AND cm.brand IN (N'Mercedes-Benz', N'Porsche')
  AND NOT EXISTS (
      SELECT 1
      FROM product_car_models pcm
      WHERE pcm.product_id = p.id
        AND pcm.car_model_id = cm.car_model_id
  );

SELECT
    (SELECT COUNT(*) FROM car_model) AS total_car_models,
    (SELECT COUNT(*) FROM product_car_models) AS total_product_car_model_links;
