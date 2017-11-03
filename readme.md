# activenet-photo-sync (aps)

`activenet-photo-sync` is a CLI tool can be used to batch upload images into [ActiveNet](http://www.activenetwork.com/solutions/active-net). It it multithreaded and can use multiple workers to process images quickly. It was able to upload  ~42,000 / 17GB student and faculty images for the [University of Guelph's Athletic Department](http://gryphons.ca) in about 15 minutes and should be adaptable for other facilities which use ActiveNet's backend.


## Requirements

* Java JRE 7/8/9


## Building a distributable executable (shaded JAR)

The application can be compiled and packaged into a single executable .jar file using maven. Run the command `mvn clean package` from the project's top-level directory. The included **pom.xml** file will download all required dependencies and build a single file jar file, **aps.jar**, inside the **target** directory. A pre-build jar file is available under **Releases** on the project's GitHub page. 


## Running the Program

Images need to be named using either am ActiveNet ID or alternate key that corresponds to their account on ActiveNet. The program will crawl for them recursively so they can be organized in subdirectories.

Run the command using the command `java -jar aps.jar`. The program uses the System.console() method so it will not work inside of most IDEs.

* Files are sourced from the `images` directory located in the current working directory.
* As the images are uploaded, records are written or updated to `customers.csv`, which will be created as required in the current working directory.