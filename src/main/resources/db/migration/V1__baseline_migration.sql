CREATE TABLE IF NOT EXISTS config_values (
name VARCHAR NOT NULL,
value decimal NOT NULL,
created_at timestamp,
updated_at timestamp,
PRIMARY KEY(name)
);

CREATE TABLE IF NOT EXISTS lists (
   id INT GENERATED ALWAYS AS IDENTITY,
   name VARCHAR NOT NULL,
   list VARCHAR NOT NULL,
   active BOOLEAN,
   PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS roles (
   id INT GENERATED ALWAYS AS IDENTITY,
   name VARCHAR(100) NOT NULL,
   active BOOLEAN,
   PRIMARY KEY (id)
);

-- User script review a different way for login
CREATE TABLE IF NOT EXISTS users (
  id UUID,
  name VARCHAR NOT NULL,
  last_name VARCHAR NOT NULL,
  email VARCHAR UNIQUE NOT NULL,
  password VARCHAR NOT NULL,
  document_type INTEGER,
  document VARCHAR NOT NULL,
  address VARCHAR,
  role_id INT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  permission_chain TEXT NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_role
        FOREIGN KEY(role_id)
            REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS people (
   id UUID UNIQUE,
   name VARCHAR(50) NOT NULL,
   last_name VARCHAR(50) NOT NULL,
   document_type INTEGER NOT NULL,
   document VARCHAR(10) UNIQUE NOT NULL,
   issue_document_date date,
   birthday date NOT NULL,
   gender_id INTEGER,
   sex_id INTEGER,
   address VARCHAR(100),
   locality_id INTEGER,
   neighborhood VARCHAR(100),
   phone VARCHAR(20),
   cell_phone VARCHAR(21),
   email VARCHAR UNIQUE NOT NULL,
   population_group_id INTEGER,
   ethnic_group_id INTEGER,
   disability_id INTEGER,
   active BOOLEAN DEFAULT TRUE,
   armed_conflict_victim BOOLEAN DEFAULT TRUE,
   displaced BOOLEAN DEFAULT FALSE,
   terms_acceptance BOOLEAN DEFAULT TRUE,
   single_mother BOOLEAN DEFAULT FALSE,
   belongs_organization BOOLEAN default false,
   created_at TIMESTAMP,
   updated_at TIMESTAMP,
   PRIMARY KEY (id),
   CONSTRAINT fk_document_type
           FOREIGN KEY(document_type)
               REFERENCES lists(id),
   CONSTRAINT fk_gender
           FOREIGN KEY(gender_id)
               REFERENCES lists(id),
   CONSTRAINT fk_locality
           FOREIGN KEY(locality_id)
               REFERENCES lists(id),
   CONSTRAINT fk_population_group
           FOREIGN KEY(population_group_id)
               REFERENCES lists(id),
   CONSTRAINT fk_ethnic_group
           FOREIGN KEY(ethnic_group_id)
               REFERENCES lists(id),
   CONSTRAINT fk_disability
           FOREIGN KEY(disability_id)
               REFERENCES lists(id),
   CONSTRAINT fk_sex
           FOREIGN KEY(sex_id)
               REFERENCES lists(id)
);

CREATE TABLE IF NOT EXISTS property_information (
    people_id uuid NOT NULL,
    address VARCHAR(100) NOT NULL,
    name VARCHAR(50) NOT NULL,
    department_id INT NOT NULL,
    city_id INT NOT NULL,
    lane varchar(100),
    hectares INT NOT NULL,
    PRIMARY KEY (people_id),
    CONSTRAINT fk_department
        FOREIGN KEY(department_id)
            REFERENCES lists(id),
    CONSTRAINT fk_city
        FOREIGN KEY(city_id)
            REFERENCES lists(id)
);

CREATE TABLE IF NOT EXISTS organization_belonging_information (
    people_id uuid NOT NULL,
    type INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    nit VARCHAR(17),
    PRIMARY KEY (people_id),
    CONSTRAINT fk_organization_type
        FOREIGN KEY(type)
            REFERENCES lists(id)
);

CREATE TABLE IF NOT EXISTS payment_information (
    people_id uuid NOT NULL,
    payment_type INT NOT NULL,
    branch_id INT,
    bank_id INT,
    account_bank_type_id INT,
    account_number VARCHAR(16),
    PRIMARY KEY (people_id),
    CONSTRAINT fk_payment_type
        FOREIGN KEY(payment_type)
            REFERENCES lists(id),
    CONSTRAINT fk_bank_id
        FOREIGN KEY(bank_id)
            REFERENCES lists(id),
    CONSTRAINT fk_branch_id
        FOREIGN KEY(branch_id)
            REFERENCES lists(id),
    CONSTRAINT fk_account_bank_type_id
        FOREIGN KEY(account_bank_type_id)
            REFERENCES lists(id)
);

CREATE TABLE IF NOT EXISTS products (
   id INT GENERATED ALWAYS AS IDENTITY,
   name VARCHAR NOT NULL,
   crop_group_id INT NOT NULL,
   percentage DECIMAL(5, 4) NOT NULL,
   maximum_to_subsidize DECIMAL(7,0) NOT NULL,
   minimum_to_apply INT NOT NULL,
   active BOOLEAN NOT NULL,
   created_at TIMESTAMP,
   updated_at TIMESTAMP,
   PRIMARY KEY (id),
   CONSTRAINT fk_crop_group
     FOREIGN KEY(crop_group_id)
         REFERENCES lists(id)
);

CREATE TABLE IF NOT EXISTS supplies (
   id INT GENERATED ALWAYS AS IDENTITY,
   name VARCHAR NOT NULL,
   price DECIMAL NOT NULL,
   crop_group_id INT NOT NULL,
   active BOOLEAN NOT NULL,
   created_at TIMESTAMP,
   updated_at TIMESTAMP,
   PRIMARY KEY (id),
   CONSTRAINT fk_crop_group
     FOREIGN KEY(crop_group_id)
         REFERENCES lists(id)
);

CREATE TABLE IF NOT EXISTS storages (
    id INT GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(50) NOT NULL,
    document VARCHAR(17) NOT NULL,
    address VARCHAR(100),
    register_number VARCHAR(100),
    activity_id INT,
    department_id INT NOT NULL,
    city_id INT NOT NULL,
    active BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_activity
      FOREIGN KEY(activity_id)
          REFERENCES lists(id),
    CONSTRAINT fk_department
      FOREIGN KEY(department_id)
          REFERENCES lists(id),
    CONSTRAINT fk_city
      FOREIGN KEY(city_id)
          REFERENCES lists(id)
);

CREATE TYPE RequestType AS ENUM ('BILL_RETURN_REQUEST');
CREATE TYPE RequestState AS ENUM ('Created',
'InReview',
'Approved',
'NonPaid',
'Paid',
'Completed',
'Rejected',
'Frozen',
'Canceled');

CREATE TABLE IF NOT EXISTS people_requests (
   id UUID UNIQUE,
   people_id UUID NOT NULL,
   type RequestType NOT NULL,
   state RequestState NOT NULL,
   active BOOLEAN DEFAULT TRUE,
   created_at TIMESTAMP,
   updated_at TIMESTAMP,
   PRIMARY KEY (id),
   CONSTRAINT fk_people_id
           FOREIGN KEY(people_id)
               REFERENCES people(id)
);

CREATE TABLE IF NOT EXISTS bill_returns (
   id UUID UNIQUE NOT NULL,
   people_request_id UUID NOT NULL,
   product_id INT NOT NULL,
   storage_id INT NOT NULL,
   PRIMARY KEY (id, people_request_id, product_id),
   CONSTRAINT fk_people_request_id
      FOREIGN KEY(people_request_id)
          REFERENCES people_requests(id),
   CONSTRAINT fk_storage
      FOREIGN KEY(storage_id)
          REFERENCES storages(id),
   CONSTRAINT fk_product
     FOREIGN KEY(product_id)
         REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS bill_returns_supplies (
    bill_return_id UUID NOT NULL,
    supply_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    bought_date date,
    presentation INT,
    unit INT NOT NULL,
    value DECIMAL NOT NULL,
    PRIMARY KEY (bill_return_id, supply_id),
    CONSTRAINT fk_bill_return
      FOREIGN KEY(bill_return_id)
          REFERENCES bill_returns(id),
    CONSTRAINT fk_supply
      FOREIGN KEY(supply_id)
          REFERENCES supplies(id)
);

CREATE TYPE AttachmentState AS ENUM ('InReview', 'Approved', 'Rejected');

CREATE TABLE IF NOT EXISTS bill_returns_attachments (
   id UUID UNIQUE NOT NULL,
   bill_return_id UUID NOT NULL,
   file_id INTEGER NOT NULL,
   name VARCHAR(255) NOT NULL,
   path VARCHAR(255) NOT NULL,
   state AttachmentState,
   created_at TIMESTAMP,
   PRIMARY KEY (id, bill_return_id, file_id),
   CONSTRAINT fk_bill_return
      FOREIGN KEY(bill_return_id)
          REFERENCES bill_returns(id),
   CONSTRAINT fk_file_id
           FOREIGN KEY(file_id)
               REFERENCES lists(id)
);

CREATE TABLE IF NOT EXISTS bill_returns_validation_attachments_events (
   id BIGSERIAL,
   bill_returns_attachment_id UUID NOT NULL,
   observation TEXT,
   reason_id INTEGER NOT NULL,
   user_id UUID NOT NULL,
   created_at TIMESTAMP,
   PRIMARY KEY (id),
    CONSTRAINT fk_bill_returns_attachment
        FOREIGN KEY(bill_returns_attachment_id)
            REFERENCES bill_returns_attachments(id),
    CONSTRAINT fk_users
        FOREIGN KEY(user_id)
            REFERENCES users(id),
    CONSTRAINT fk_reason_id
        FOREIGN KEY(reason_id)
            REFERENCES lists(id)
);