# activenet-photo-sync (aps)

`activenet-photo-sync` is a tool can be used to batch upload images into [ActiveNet](http://www.activenetwork.com/solutions/active-net). It it multithreaded and can use multiple workers to process images quickly. It was able to upload  ~42,000 / 17GB student and faculty images for the [University of Guelph's Athletic Department](http://gryphons.ca) in about 15 minutes and should be adaptable for other facilities which use ActiveNet's backend.


## Requirements

* Java JRE 7/8


## Building a distributable executable (shaded JAR)

The application can be compiled and packaged into a single executable .jar file using maven. Run the command `mvn clean package` from the project's top-level directory. The included **pom.xml** file will download all required dependencies and build a single file jar file, **aps.jar**, inside the **target** directory.   


## Running the Program

Images need to be named using either am ActiveNet ID or alternate key that corresponds to their account on ActiveNet. The program will crawl for them recursively so they can be organized in subdirectories.

When running the program you will need to provide credentials for a MySQL server created with a database named 'aps' containing a table named customer, which can be created like follows:

```SQL
CREATE DATABASE aps;
GRANT ALL PRIVILEGES ON aps.* TO '[username]'@'[domain]' IDENTIFIED BY '[password]';
FLUSH PRIVILEGES;

CREATE TABLE customer
(
    primaryID BIGINT(20) PRIMARY KEY NOT NULL,
    activeID BIGINT(20) NOT NULL,
    name VARCHAR(45),
    email VARCHAR(100),
    uploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

These credentials must also be able to log into ActiveNet and have sufficient priviledges to make account changes.


## Record Keeping

* As the images are uploaded, records are written or updated to the MySQL database.
* This database will be populated with customer names, emails, ActiveNet IDs, as images are uploaded, along with a timestamp.
* This database is queried prior to uploading so that uploaded images are skipped on subsequent runs.
