CREATE TABLE IF NOT EXISTS CustomerDetails
(
  id VARCHAR(255) NOT NULL,
  region VARCHAR(255),
  PRIMARY KEY (id)
);

INSERT INTO CustomerDetails SELECT 'cust1', 'Virginia' from dual WHERE NOT EXISTS(SELECT * FROM CustomerDetails WHERE id = 'cust1');
INSERT INTO CustomerDetails SELECT 'cust2', 'NewYork' from dual WHERE NOT EXISTS(SELECT * FROM CustomerDetails WHERE id = 'cust2');
INSERT INTO CustomerDetails SELECT 'cust3', 'Virginia' from dual WHERE NOT EXISTS(SELECT * FROM CustomerDetails WHERE id = 'cust3');

CREATE TABLE IF NOT EXISTS OrderDetails
(
  orderId VARCHAR(255) NOT NULL,
  customerId VARCHAR(255),
  orderTotal NUMERIC(10,2),
  createdOn DATETIME,
  PRIMARY KEY (orderId)
);

INSERT INTO OrderDetails SELECT 'order-1a', 'cust1', 103.72, '2020-08-30 16:30:11' WHERE NOT EXISTS(SELECT * FROM OrderDetails WHERE orderId = 'order-1a');
INSERT INTO OrderDetails SELECT 'order-1b', 'cust1', 84.11, '2020-09-08 16:30:11' WHERE NOT EXISTS(SELECT * FROM OrderDetails WHERE orderId = 'order-1b');
INSERT INTO OrderDetails SELECT 'order-1c', 'cust1', 97.36, '2020-09-08 16:30:11' WHERE NOT EXISTS(SELECT * FROM OrderDetails WHERE orderId = 'order-1c');
INSERT INTO OrderDetails SELECT 'order-2a', 'cust2', 17.82, '2020-08-25 16:30:11' WHERE NOT EXISTS(SELECT * FROM OrderDetails WHERE orderId = 'order-2a');
INSERT INTO OrderDetails SELECT 'order-2b', 'cust2', 43.61, '2020-08-26 16:30:11' WHERE NOT EXISTS(SELECT * FROM OrderDetails WHERE orderId = 'order-2b');
INSERT INTO OrderDetails SELECT 'order-3a', 'cust3', 9.35, '2020-08-26 16:30:11' WHERE NOT EXISTS(SELECT * FROM OrderDetails WHERE orderId = 'order-3a');
INSERT INTO OrderDetails SELECT 'order-3b', 'cust3', 78.87, '2020-09-09 16:30:11' WHERE NOT EXISTS(SELECT * FROM OrderDetails WHERE orderId = 'order-3b');
