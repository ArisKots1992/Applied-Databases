#!/bin/bash

#Drop all the tables if they exists inside the "ad" Database
#and check if the DB exists
mysql  < drop.sql
echo "Tables Dropped."

#create the Database
mysql < create.sql
echo "Database and Tables Created."

#Compile the program
javac MySAX.java

#Execute the program and generate .csv
java MySAX ../ebay_data/items-*.xml
echo "CSV files created."

#Load from .csv files
mysql ad < load.sql
echo "CSV files Loaded to Database"

#Delete all the .csv and .class files.
rm *.csv
rm *.class
echo "CSV files Deleted."
