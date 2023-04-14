CREATE TABLE IF NOT EXISTS temp_user_product (
request_number INT NOT NULL,
document VARCHAR(100) NOT NULL,
product_id INT NOT NULL,
PRIMARY KEY(request_number, document, product_id),
CONSTRAINT fk_people
    FOREIGN KEY(document)
        REFERENCES people(document),
CONSTRAINT fk_product
    FOREIGN KEY(product_id)
        REFERENCES products(id)
)