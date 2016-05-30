import java.io.StringReader;
import java.io.File;
import java.nio.file.*;
import java.lang.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import java.io.*;
import java.nio.file.Path;

public class Searcher implements Comparator<Item> {

	public Searcher() {
	}

	static double latitude,longitude,width;
	static Point[] box = null;
	static boolean basicSearch;
	public static void main(String[] args) throws Exception {
		String usage = "java Searcher";

		if(args.length == 1){
			basicSearch = true;
			basicSearch(args[0], "indexes");
		}
		else if(args.length == 7){
			// check if we have 6 arguments
			if( args[1].equals("-x") && args[3].equals("-y") && args[5].equals("-w") ){

				latitude = Double.valueOf(args[4]);
				longitude = Double.valueOf(args[2]);
				width = Double.valueOf(args[6]);
				Coordinates coord = new Coordinates();
				// Here we check if the width for the specific latitude and longitude is velid to vreate the box
				if (coord.canCreateBox(latitude, longitude, width)) {
					box = coord.createBoundBox(latitude, longitude, width);
					// for (int i = 0; i < box.length; i++)
					// 	box[i].printPoint();

					SpatialSearch(args[0],"indexes");
				}else{
					System.out.println("Please select a correct Width or Lat/Lon.");
				}

			}else{
				System.out.println("Wrong arguments please use the form:\n java Searcher \"pattern\" -x longitude -y latitude -w width \n " +
				"or \n java Searcher \"pattern\" ");
			}
		}else{
			System.out.println("Wrong arguments please use the form:\n java Searcher \"pattern\" -x longitude -y latitude -w width \n " +
			"or \n java Searcher \"pattern\" ");
		}

	}

	private static TopDocs basicSearch(String searchText, String p) {

		System.out.println("Running Basic Search(" + searchText + ")");
		Connection dbConnection = null;
		PreparedStatement preparedStatement = null;
		ArrayList<Item> itemList = new ArrayList<Item>();

		try {
			Path path = Paths.get(p);
			Directory directory = FSDirectory.open(path);
			IndexReader indexReader = DirectoryReader.open(directory);
			IndexSearcher indexSearcher = new IndexSearcher(indexReader);
			QueryParser queryParser = new QueryParser("content", new SimpleAnalyzer());
			Query query = queryParser.parse(searchText);
			TopDocs topDocs = indexSearcher.search(query, 20000);

			// create a connection to the database
			dbConnection = DbManager.getConnection(true);

			// we can avoid SQL injection with this method
			String SQLquery = "SELECT current_price FROM auction WHERE item_id = ?";

			float prev_score = -1;
			// order score higest first and lower current_price on score equality
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document document = indexSearcher.doc(scoreDoc.doc);
				String item_id = document.get("item_id");
				String item_name = document.get("item_name");
				float score = scoreDoc.score;
				float current_price;

				// get the current price for the specific item
				preparedStatement = dbConnection.prepareStatement(SQLquery);
				preparedStatement.setString(1,item_id);
				ResultSet r = preparedStatement.executeQuery();
				r.next();
				current_price = r.getInt("current_price");

				if( prev_score != score && prev_score != -1){
					Collections.sort(itemList, new Searcher());
					for(int i=0; i<itemList.size(); i++)
						itemList.get(i).printItem();
					itemList.clear();
				}

				// create an item and add to list
				Item item = new Item(item_id,item_name,score,current_price);
				itemList.add(item);

				prev_score = score;
			}
			// we never forget the last Item/Items
			if(prev_score != -1){
				Collections.sort(itemList, new Searcher());
				for(int i=0; i<itemList.size(); i++)
					itemList.get(i).printItem();
				itemList.clear();
			}
			// display the total hits
			System.out.println("Number of Hits: " + topDocs.totalHits);

			return topDocs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			try {
				// close Everything
				if(preparedStatement != null)
					preparedStatement.close();
				dbConnection.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	private static TopDocs SpatialSearch(String searchText, String p) {

		System.out.println("Running Spatial Search(" + searchText + ")");
		Connection dbConnection = null;
		PreparedStatement preparedStatement = null;
		ArrayList<Item> itemList = new ArrayList<Item>();

		try {
			Path path = Paths.get(p);
			Directory directory = FSDirectory.open(path);
			IndexReader indexReader = DirectoryReader.open(directory);
			IndexSearcher indexSearcher = new IndexSearcher(indexReader);
			QueryParser queryParser = new QueryParser("content", new SimpleAnalyzer());
			Query query = queryParser.parse(searchText);
			TopDocs topDocs = indexSearcher.search(query, 20000);

			// create a connection to the database
			dbConnection = DbManager.getConnection(true);
			String SQLquery = "SELECT current_price FROM auction WHERE item_id = ?";

			//create the polygon and the function
			String selectFromPoly = "SELECT item_id " +
									"FROM item_coordinates_point " +
									"WHERE MBRContains" +
									"(GeomFromText('Polygon((" +
									String.valueOf(box[0].latitude) + " " + String.valueOf(box[0].longitude) + "," +
									String.valueOf(box[1].latitude) + " " + String.valueOf(box[1].longitude) + "," +
									String.valueOf(box[2].latitude) + " " + String.valueOf(box[2].longitude) + "," +
									String.valueOf(box[3].latitude) + " " + String.valueOf(box[3].longitude) + "," +
									String.valueOf(box[0].latitude) + " " + String.valueOf(box[0].longitude) + "))')" +
									", coordinates) and item_id= ?";
			// get distance for an item_id
			String selectDistance =  "SELECT X(coordinates),Y(coordinates)," +
			"((ACOS( SIN(X(coordinates)*PI()/180) *" +
			"SIN(?*PI()/180) + COS(X(coordinates)*PI()/180)* " +
			"COS(?*PI()/180) * COS((Y(coordinates)-(?" +
			"))*PI()/180))*180/PI())*60*1.1515/0.62137) AS distance FROM item_coordinates_point WHERE item_id= ?";

			float prev_score = -1;
			int countHits = 0;
			// order score higest first and lower current_price on score equality
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document document = indexSearcher.doc(scoreDoc.doc);
				String item_id = document.get("item_id");
				String item_name = document.get("item_name");
				float score = scoreDoc.score;
				double current_price;
				double distance;
				// here we skip every item that is not in our boundBox
				PreparedStatement checkBounds = dbConnection.prepareStatement(selectFromPoly);
				checkBounds.setString(1,item_id);
				ResultSet r_spatial = checkBounds.executeQuery();
				if(!r_spatial.next())
					continue;
				checkBounds.close();

				// get the current price for the specific item
				preparedStatement = dbConnection.prepareStatement(SQLquery);
				preparedStatement.setString(1,item_id);
				ResultSet r = preparedStatement.executeQuery();
				r.next();
				current_price = r.getDouble("current_price");
				countHits++;
				// get the distance for the specific item
				PreparedStatement getDistance = dbConnection.prepareStatement(selectDistance);
				getDistance.setString(1,String.valueOf(latitude));
				getDistance.setString(2,String.valueOf(latitude));
				getDistance.setString(3,String.valueOf(longitude));
				getDistance.setString(4,item_id);

				ResultSet r_distance = getDistance.executeQuery();
				// we are sure there would be a distance because there is a lat lon otherwise i would be rejected fro
				// the bound checkBounds
				r_distance.next();
				distance = r_distance.getDouble("distance");

				if( prev_score != score && prev_score != -1){
					Collections.sort(itemList, new Searcher());
					for(int i=0; i<itemList.size(); i++)
						itemList.get(i).printItemWithDistance();
					itemList.clear();
				}

				// create an item and add to list
				Item item = new Item(item_id,item_name,score,current_price,distance);
				itemList.add(item);

				prev_score = score;
			}
			// we never forget the last Item/Items
			if(prev_score != -1){
				Collections.sort(itemList, new Searcher());
				for(int i=0; i<itemList.size(); i++)
					itemList.get(i).printItemWithDistance();
				itemList.clear();
			}
			System.out.println("Number of Hits: " + countHits);

			return topDocs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			try {
				// close Everything
				if(preparedStatement != null)
					preparedStatement.close();
				dbConnection.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
/**
 * Function to override the comparing operation in order to use Collections.sort
 * @param item1
 * @param item2
 * @return
 */
	@Override
	public int compare(Item item1, Item item2) {
		// TODO Auto-generated method stub
		if ( basicSearch ){
			if(item1.current_price > item2.current_price)
				return 1;
			else if(item1.current_price == item2.current_price)
				return 0;
			else
				return -1;
		}else{
			if( item1.distance > item2.distance)
				return 1;
			else if(item1.distance == item2.distance){
				if(item1.current_price > item2.current_price)
					return 1;
				else if(item1.current_price == item2.current_price)
					return 0;
				else
					return -1;
			}
			else
				return -1;
		}

	}
}

/*******************************************************/
/*********************** CLASS Item ********************/
/*******************************************************/
// custom class in order to store and sort the Items
class Item{
	String item_id;
	String item_name;
	float score;
	double current_price;
	double distance;
	Item(String item_id, String item_name, float score, double current_price){
		this.item_id = item_id;
		this.item_name = item_name;
		this.score = score;
		this.current_price = current_price;
	}
	Item(String item_id, String item_name, float score, double current_price,double distance){
		this.item_id = item_id;
		this.item_name = item_name;
		this.score = score;
		this.current_price = current_price;
		this.distance = distance;
	}
	public void printItem(){
		String ANSI_RESET = "\u001B[0m";
		String ANSI_CYAN = "\u001B[36m";
		String  ANSI_YELLOW = "\u001B[33m";
		String ANSI_BLUE = "\u001B[34m";
		System.out.println(item_id + "|" + item_name + "|" + score + "|" + current_price +"$");
	}
	public void printItemColor(){
		String ANSI_RESET = "\u001B[0m";
		String ANSI_CYAN = "\u001B[36m";
		String  ANSI_YELLOW = "\u001B[33m";
		String ANSI_BLUE = "\u001B[34m";
		System.out.println(item_id + " [" + item_name + "]" + ", score: "+ ANSI_YELLOW + score + ANSI_RESET + ", " + ANSI_CYAN + current_price + ANSI_RESET +"$");
	}
	public void printItemWithDistance(){
		String ANSI_RESET = "\u001B[0m";
		String ANSI_CYAN = "\u001B[36m";
		String  ANSI_YELLOW = "\u001B[33m";
		String ANSI_BLUE = "\u001B[34m";

		System.out.println(item_id + "|" + item_name + "|" + score + "|" + current_price + "$|" + distance);
	}

	public void printItemWithDistanceColor(){
		String ANSI_RESET = "\u001B[0m";
		String ANSI_CYAN = "\u001B[36m";
		String  ANSI_YELLOW = "\u001B[33m";
		String ANSI_BLUE = "\u001B[34m";

		System.out.println(item_id + " [" + item_name + "]" + ", Score: "+ ANSI_YELLOW + score + ANSI_RESET +", " +  ANSI_CYAN + current_price + ANSI_RESET + "$, Distance:" + ANSI_BLUE + distance + ANSI_RESET);
	}
}

/*******************************************************/
/******************** CLASS Coordinates ****************/
/*******************************************************/
// custom class in order to create precisely the bounding box
class Coordinates {

	public boolean canCreateBox(double lat, double lon, double width){

		// if you request a bounding box larger than the availiable height
		if( width/2 > getDistanceInKm(lat, lon, 90, lon)  )
			return false;
		if( width/2 > getDistanceInKm(lat, lon, -90, lon)  )
			return false;

		return true;
		}
/*
 * Create a square given the center point and the width of the square
 */
	public Point[] createBoundBox(double lat, double lon, double width){

		Point[] points = new Point[4];
		double temp_lat;
		double temp_lon;

		// Top Left corner
		double up_lat = findPointNorth(lat,lon,width/2);			// UP
		double up_lon = lon;

		double down_lat = findPointSouth(lat,lon,width/2);
		double down_lon = lon;

		double left_lon = findPointWest(lat,lon,width/2);
		double left_lat = lat;

		double right_lon = findPointEast(lat,lon,width/2);
		double right_lat = lat;

		points[0] = new Point(up_lat, left_lon);
		points[1] = new Point(up_lat, right_lon);
		points[2] = new Point(down_lat, right_lon);
		points[3] = new Point(down_lat, left_lon);

		return points;
	}
	public double getDistanceInKm(double lat1, double lon1, double lat2, double lon2){
		double PI = Math.PI;
		double distance = (Math.acos(
									Math.sin(lat1 * PI/180.0) *
									Math.sin(lat2 * PI/180.0) +
									Math.cos(lat1 * PI/180.0) *
									Math.cos(lat2 * PI/180.0) *
									Math.cos((lon1-lon2) * PI/180.0)
									)  *  180/PI
							) * 60*1.1515/0.62137;
		return distance;
	}

/**
 * find a point (distance)Km North(above) of the lat,lon point. Longitude is constant
 * @param lat
 * @param lon
 * @param distance
 * @return new latitude
 */
	public double findPointNorth(double lat, double lon, double distance){
		double lat_new = lat;
		while( getDistanceInKm(lat, lon, lat_new, lon) < distance){
			lat_new += 0.005;
			if( lat_new > 90){
				return 90;
			}
		}
		return lat_new;
	}

/**
 * find a point (distance)Km South(under) of the lat,lon point. Longitude is constant
 * @param lat
 * @param lon
 * @param distance
 * @return new latitude
 */
	public double findPointSouth(double lat, double lon, double distance){
		double lat_new = lat;
		while( getDistanceInKm(lat, lon, lat_new, lon) < distance){
			lat_new -= 0.005;
			if( lat_new < -90){
				return -90;
			}
		}
		return lat_new;
	}
/**
 * find a point (distance)Km East(right) of the lat,lon point. Latitude is constant
 * @param lat
 * @param lon
 * @param distance
 * @return new longitude
 */
	public double findPointEast(double lat, double lon, double distance){
		double lon_new = lon;
		double max_lon = lon;
		int count =0;
		double max = -1; // in case there is no such a distance on the right so return the max longitude
		double dist;
		while( (dist=getDistanceInKm(lat, lon, lat, lon_new)) < distance){
			if(dist > max){
				max = dist;
				max_lon = lon_new;
			}

			lon_new += 0.005;
			if( lon_new >= 180){
				count++;
				lon_new = -180;
			}
			if(count == 2)
				return max_lon;
		}
		return lon_new;
	}
/**
 * find a point (distance)Km East(right) of the lat,lon point. Latitude is constant
 * @param lat
 * @param lon
 * @param distance
 * @return new longitude
 */
	public double findPointWest(double lat, double lon, double distance){
		double lon_new = lon;
		double max_lon = lon;
		int count =0;
		double max = -1; // in case there is no such a distance on the left so return the max longitude
		double dist;
		while( (dist=getDistanceInKm(lat, lon, lat, lon_new)) < distance){
			lon_new -= 0.005;
			if(dist > max){
				max = dist;
				max_lon = lon_new;
			}
			if( lon_new <= -180){
				count ++;
				lon_new = 180;
			}
			if(count == 2)
				return max_lon;
		}
		return lon_new;
	}
}
/*******************************************************/
/*********************** CLASS Point *******************/
/*******************************************************/
// custom class fo my Point Objects
class Point{
	double latitude;
	double longitude;
	Point(double lat,double lon){
		this.latitude = lat;
		this.longitude = lon;
	}
	public void printPoint(){
		System.out.println(latitude + "," + longitude);
	}
}

