CREATE TABLE DEPARTMENT 
(
      DEPARTMENT_ID NUMERIC  NOT NULL,
      DEPARTMENT_NAME VARCHAR ,
      LOCATION_ID NUMERIC ,
      MANAGER_ID NUMERIC ,
    CONSTRAINT DEPARTMENT_PK PRIMARY KEY(DEPARTMENT_ID)
);
CREATE TABLE EMPLOYEE 
(
      EMAIL VARCHAR ,
      EMPLOYEE_ID NUMERIC  NOT NULL,
      FIRST_NAME VARCHAR ,
      HIRE_DATE DATE ,
      JOB_ID VARCHAR ,
      LAST_NAME VARCHAR ,
      PHONE_NUMBER VARCHAR ,
      SALARY NUMERIC ,
      DEPARTMENT_ID NUMERIC ,
    CONSTRAINT EMPLOYEE_PK PRIMARY KEY(EMPLOYEE_ID)
);

CREATE TABLE COUNTRY 
(
      COUNTRY_ID VARCHAR  NOT NULL,
      COUNTRY_NAME VARCHAR ,
      REGION_ID NUMERIC ,
    CONSTRAINT COUNTRY_PK PRIMARY KEY(COUNTRY_ID)
);
CREATE TABLE LOCATION 
(
      LOCATION_ID NUMERIC  NOT NULL,
      STREET_ADDRESS VARCHAR ,
      POSTAL_CODE VARCHAR ,
      CITY VARCHAR ,
      STATE_PROVINCE VARCHAR ,
      COUNTRY_ID VARCHAR ,
    CONSTRAINT LOCATION_PK PRIMARY KEY(LOCATION_ID)
);
CREATE TABLE REGION 
(
      REGION_ID NUMERIC  NOT NULL,
      REGION_NAME VARCHAR ,
    CONSTRAINT REGION_PK PRIMARY KEY(REGION_ID)
);

CREATE TABLE DATA_SYNCH_ACTIONS 
(
      ID NUMERIC NOT NULL,
      SERVICE_CLASS_NAME VARCHAR NOT NULL,
      ENTITY_CLASS_NAME VARCHAR NOT NULL,
      JSON_PAYLOAD VARCHAR ,
      ACTION VARCHAR NOT NULL,
      DATE_CREATED DATE NOT NULL,
      DATE_LAST_SYNCH DATE NOT NULL,
      LAST_SYNCH_ERROR VARCHAR,
      CUSTOM_METHOD_NAME VARCHAR,
    CONSTRAINT DSA_PK PRIMARY KEY(SERVICE_CLASS_NAME ,ID)
);
