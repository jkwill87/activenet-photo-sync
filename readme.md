# gryph-photo-upload (GPU)

The `gryph-photo-upload` tool can be used to batch upload client images into [ActiveNet](http://www.activenetwork.com/solutions/active-net). It was used to upload ~30,000 student and faculty images for the [University of Guelph's Athletic Department](http://gryphons.ca), however could possibly be adapted for other facilities which use ActiveNet's backend.


## Requirements

* Windows 7+ (for compatibility with ActiveNet's Java applet)
* ActiveNet added to Internet Explorer's trusted sites (see below)
* Java installed and updated (see below)


## Getting Started

### Add ActiveNet to Internet Explorer's trusted sites list

In order for [Selenium](http://www.seleniumhq.org/) to work properly, ActiveNet needs to be added to IE's trusted sites list. This can be done by following these steps:

1. In Internet Explorer press the Alt key to bring up the menu bar and navigate to **Tools > Internet Options**.
2. In the options menu navigate to the **Security** tab, click on the **Trusted Sites** icon (with the green checkmark) and then click the **Sites** button underneath.
3. Lastly enter `https://anprodca.active.com` into the field labeled **Add this website to the zone:**. Click **Add** and then **Close** to commit your change.


### Ensure Java is up to date

ActiveNet uses a Java applet to upload photos. In order for the program to progress autonomously the applet must be able to load and be interacted with directly, using [SilukiX](http://www.sikulix.com/), without user intervention.

1. Make sure you have the latest version of Java installed and are [able to run it in the browser]( https://www.java.com/en/download/help/testvm.xml).  
2. Try to manually upload an image through ActiveNet, when the upload window presents itself, click the option to **always run Java from the site**.    


## Building a distributable executable (shaded JAR)

After downloading and installing maven for Windows and setting up the appropriate Java and Maven system environment paths, the application can be compiled and packaged into a single executable .jar file using the command `mvn clean package` from the project's top-level directory. The included **pom.xml** file will download all required dependencies and build a single file jar file, **gryph-photo-upload.jar**, inside the **target** directory.   


## Running the Program

The program runs through a series of 3 steps:

1. **Select the image directory that you will be sourcing images from.** The program will search recursively from this directory for any jpeg images that include a student or staff number in the file name, padded by up to 4 zeros. If other files are included in the directory its fine, they will just be ignored.
2. **Enter your login credentials.** These are the same one used to log in to "activenetca.active.com/uofg/login".
3. **Begin Upload.** Click upload to begin the automation process. Progress will be displayed in the GUI and will notify you when uploading has finished. You can pause at anytime.


## Record Keeping

* As the images are uploaded, records are written or updated to a SQLite database file located in **resources/CustomerRecords.db**.
* This file scrapes customer names and ActiveNet ids as it uploads are processed and records the upload status a timestamp.
* This database is referenced on subsequent runs and clients who have had their images uploaded on subsequent runs will be skipped.
* The program will also skip uploading an image if the client already has one uploaded.

### SQLite Schema

The **CustomerRecods.db** file uses the following schema:
```sql
CREATE TABLE IF NOT EXISTS customers (
    universityId INTEGER NOT NULL,
    activeId INTEGER,
    name VARCHAR,
    uploaded BOOLEAN NOT NULL,
    updated TIME NOT NULL,
    PRIMARY KEY (universityId)
);
```
