# Applied-Databases

**First Assignment**  
The purpose of this assignment is to
design a database schema for a given dataset,
shred XML data into csv-files, and
create tables in a database and populate them via the csv-files.
The dataset consists of ebay auction data, given by a set of XML files. Each XML file is valid with respect to the given DTD. For Point (1) you will work out a convenient relational schema to represent the given auction XML data. This includes identifying the primary keys of each table, and explaining why the schema is "good", i.e., why it adheres to the desired normal forms of database design. Point (2) is the main part of this assignment. You are asked to write a Java program that reads in the XML files, and writes out csv-files, one for each table in your schema. In Point (3) you are asked to write SQL-scripts which create your tables in a MySQL database, and which load the csv-files into these tables. Finally, you are asked to test your database by running a few SQL-queries over it.

**Second Assignment**  
In Assignment 1 you desigend a relational schema for EBAY data (given in XML). You converted the XML files to cvs-files and loaded them into your tables under a MySQL database named "ad". The purpose of this assignment is to
use JDBC to fetch data from your "ad" database,
insert this data into a Lucene index for full text search, and
provide a search function that combines Lucene's full text search with MySQL's spatial queries.
To efficiently carry out spatial search in MySQL, you are asked to convert the longitude and latitude information of itmes into points (i.e., into MySQL's POINT data type), and to build a spacial index over those points.


*Programmed in 2016**
