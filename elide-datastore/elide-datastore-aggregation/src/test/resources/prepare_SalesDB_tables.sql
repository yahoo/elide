CREATE TABLE IF NOT EXISTS customer_details
(
  id VARCHAR(255) NOT NULL,
  name VARCHAR(255),
  zip_code INT,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS region_details
(
  zip_code INT NOT NULL,
  region VARCHAR(255) NOT NULL,
  PRIMARY KEY (zip_code)
);

CREATE TABLE IF NOT EXISTS sales_performance
(
   employee_id VARCHAR(255) NOT NULL,
   sales INT,
   PRIMARY KEY (employee_id)
);

INSERT INTO customer_details SELECT 'cust1', 'foo1', 20166 from dual WHERE NOT EXISTS(SELECT * FROM customer_details WHERE id = 'cust1');
INSERT INTO customer_details SELECT 'cust2', 'foo2', 10002 from dual WHERE NOT EXISTS(SELECT * FROM customer_details WHERE id = 'cust2');
INSERT INTO customer_details SELECT 'cust3', 'foo3', 20170 from dual WHERE NOT EXISTS(SELECT * FROM customer_details WHERE id = 'cust3');

INSERT INTO region_details SELECT 20166, 'Virginia' from dual WHERE NOT EXISTS(SELECT * FROM region_details WHERE zip_code = 20166);
INSERT INTO region_details SELECT 20170, 'Virginia' from dual WHERE NOT EXISTS(SELECT * FROM region_details WHERE zip_code = 20170);
INSERT INTO region_details SELECT 10002, 'NewYork' from dual WHERE NOT EXISTS(SELECT * FROM region_details WHERE zip_code = 10002);

CREATE TABLE IF NOT EXISTS order_details
(
  order_id VARCHAR(255) NOT NULL,
  customer_id VARCHAR(255),
  order_total NUMERIC(10,2),
  created_on DATETIME,
  PRIMARY KEY (order_id)
);

INSERT INTO order_details SELECT 'order-1a', 'cust1', 103.72, '2020-08-30 16:30:11' WHERE NOT EXISTS(SELECT * FROM order_details WHERE order_id = 'order-1a');
INSERT INTO order_details SELECT 'order-1b', 'cust1', 84.11, '2020-09-08 16:30:11' WHERE NOT EXISTS(SELECT * FROM order_details WHERE order_id = 'order-1b');
INSERT INTO order_details SELECT 'order-1c', 'cust1', 97.36, '2020-09-08 16:30:11' WHERE NOT EXISTS(SELECT * FROM order_details WHERE order_id = 'order-1c');
INSERT INTO order_details SELECT 'order-2a', 'cust2', 17.82, '2020-08-25 16:30:11' WHERE NOT EXISTS(SELECT * FROM order_details WHERE order_id = 'order-2a');
INSERT INTO order_details SELECT 'order-2b', 'cust2', 43.61, '2020-08-26 16:30:11' WHERE NOT EXISTS(SELECT * FROM order_details WHERE order_id = 'order-2b');
INSERT INTO order_details SELECT 'order-3a', 'cust3', 9.35, '2020-08-26 16:30:11' WHERE NOT EXISTS(SELECT * FROM order_details WHERE order_id = 'order-3a');
INSERT INTO order_details SELECT 'order-3b', 'cust3', 78.87, '2020-09-09 16:30:11' WHERE NOT EXISTS(SELECT * FROM order_details WHERE order_id = 'order-3b');

CREATE TABLE IF NOT EXISTS delivery_details
(
  delivery_id VARCHAR(255) NOT NULL,
  order_id VARCHAR(255) NOT NULL,
  tracking_number BIGINT,
  courier_name VARCHAR(255),
  delivered_on DATETIME,
  PRIMARY KEY (delivery_id)
);

INSERT INTO delivery_details SELECT 'del-1a', 'order-1a', 2602407706, 'UPS', '2020-09-05 16:30:11' WHERE NOT EXISTS(SELECT * FROM delivery_details WHERE delivery_id = 'del-1a');
INSERT INTO delivery_details SELECT 'del-1b', 'order-1b', 1112021108893, 'FEDEX', '2020-09-11 16:30:11' WHERE NOT EXISTS(SELECT * FROM delivery_details WHERE delivery_id = 'del-1b');
INSERT INTO delivery_details SELECT 'del-1c', 'order-1c', 1112021722136, 'FEDEX', '2020-09-11 16:30:11' WHERE NOT EXISTS(SELECT * FROM delivery_details WHERE delivery_id = 'del-1c');
INSERT INTO delivery_details SELECT 'del-2a', 'order-2a', 1112021844256, 'FEDEX', '2020-08-30 16:30:11' WHERE NOT EXISTS(SELECT * FROM delivery_details WHERE delivery_id = 'del-2a');
INSERT INTO delivery_details SELECT 'del-2b', 'order-2b', 2602534554, 'UPS', '2020-08-31 16:30:11' WHERE NOT EXISTS(SELECT * FROM delivery_details WHERE delivery_id = 'del-2b');
INSERT INTO delivery_details SELECT 'del-3a', 'order-3a', 2602452494, 'UPS', '2020-08-31 16:30:11' WHERE NOT EXISTS(SELECT * FROM delivery_details WHERE delivery_id = 'del-3a');
INSERT INTO delivery_details SELECT 'del-3b', 'order-3b', 2602475626, 'UPS', '2020-09-13 16:30:11' WHERE NOT EXISTS(SELECT * FROM delivery_details WHERE delivery_id = 'del-3b');
