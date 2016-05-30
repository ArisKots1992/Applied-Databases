
/* Parser skeleton for processing item-???.xml files. Must be compiled in
 * JDK 1.5 or above.
 *
 * Instructions:
 *
 * This program processes all files passed on the command line (to parse
 * an entire diectory, type "java MyParser myFiles/*.xml" at the shell).
 *
 */

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.*;
import java.util.*;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

public class MySAX extends DefaultHandler {
	
	private static String DELIMITER = "|*|";
	private static String NEW_LINE_SEPARATOR = "\n";
	private StringBuilder myString = new StringBuilder(); 	// string to append and avoid &amp; 
	private boolean foundBid = false; 						// in order to know if we are inside a bid

	/* Categories & ItemHasCategory Tables */
	private Map<String, Integer> categories = new HashMap<String, Integer>();
	private int categoriesID = 1;
	/* The following HashSet is to prevent situations like the item 1310018094 where in the XML we have duplicate lines for the same item. */
	HashSet <String> innerCat = new HashSet <String>();
	
	/* Items Table */
	private String itemID = "";
	private String name = "";
	private String currently = "";
	private String buyPrice = "-1";
	private String firstBid = "";
	private String numberOfBids = "";
	private String location = "";
	private String country = "";
	private String started = "";
	private String ends = "";
	private String description = "";
	
	/* Table Sellers */
	private HashSet <String> sellers = new HashSet <String>();
	
	/* Table Bids, Bidders, ItemHasBid */
	private HashSet <String> bidders = new HashSet <String>();
	private int bidID = 1;
	private String bidderID = "";
	private String bidderRating = "";
	private String time = "";
	private String amount = "";
	private String bidderLocation = "-1";
	private String bidderCountry = "-1";
	
	/* Table Coordinates */
	private Map<String, Integer> coordinates = new HashMap<String, Integer>();
	private int coordinateID = 1;
	private String latitude = "-999";
	private String longitude = "-999";
	
	public static void main(String args[]) throws Exception {
		
		/* Create the .CSV Files */
		createCSVs();
		
		XMLReader xr = XMLReaderFactory.createXMLReader();
		MySAX handler = new MySAX();
		xr.setContentHandler(handler);
		xr.setErrorHandler(handler);

		// Parse each file provided on the
		// command line.
		for (int i = 0; i < args.length; i++) {
			FileReader r = new FileReader(args[i]);
			xr.parse(new InputSource(r));
		}
		//System.out.println("--------------");
	//	for (String key : categories.keySet())
		//	System.out.println(key + " " + categories.get(key));

	}

	public MySAX() {
		super();
	}

	/*
	 * Returns the amount (in XXXXX.xx format) denoted by a money-string like
	 * $3,453.23. Returns the input if the input is an empty string.
	 */
	static String strip(String money) {
		if (money.equals(""))
			return money;
		else {
			double am = 0.0;
			NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
			try {
				am = nf.parse(money).doubleValue();
			} catch (ParseException e) {
				System.out.println("This method should work for all " + "money values you find in our data.");
				System.exit(20);
			}
			nf.setGroupingUsed(false);
			return nf.format(am).substring(1);
		}
	}
	@SuppressWarnings("finally")
	static String transformDate(String date){
		String newDateString = "";
		try{
		String inputDateStr = date;
		String old_format = "MMM-dd-yy HH:mm:ss";
		String new_format = "yyyy-MM-dd HH:mm:ss";
		SimpleDateFormat dateFormat = new SimpleDateFormat(old_format);
		Date inputDate = dateFormat.parse(inputDateStr);
		
		dateFormat.applyPattern(new_format);
		newDateString = dateFormat.format(inputDate);
		} catch (ParseException e) {
			System.out.println("Error while Parsing Date!");
			System.exit(0);
		}finally {
			return newDateString;
		}
	}
	////////////////////////////////////////////////////////////////////
	// Event handlers.
	////////////////////////////////////////////////////////////////////

	public void startDocument() {
		//System.out.println("Start document");
	}

	public void endDocument() {
	//	System.out.println("End document");
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {

		myString.setLength(0); // clear the String in order to be ready for the
								// endElement

		if (name.equals("Item")) { // field REQUIRED, No need for check
			//System.out.println("Attribute: " + atts.getLocalName(0) + "=" + atts.getValue(0));
			itemID = atts.getValue(0);
		} 
		else if (name.equals("Bidder")) { // field REQUIRED, again
			bidderRating = atts.getValue(0);
			bidderID = atts.getValue(1);
		} 
		else if (name.equals("Location")) { // here we have to check, field is
												// IMPLIED
			for (int i = 0; i < atts.getLength(); i++){
				if ( atts.getLocalName(i).equals("Longitude"))
					longitude = atts.getValue(i);
				else if( atts.getLocalName(i).equals("Latitude"))
					latitude = atts.getValue(i);
			}
		} 
		else if (name.equals("Seller")) { // field REQUIRED, again
			String rating = atts.getValue(0);
			String userID = atts.getValue(1);
			String sellerRow = userID + DELIMITER + rating;
			if ( !sellers.contains(sellerRow) ){	//Print only if its unique to avoid duplicated lines
				sellers.add(sellerRow);
				writeToCSV("sellers.csv", sellerRow);
			}
			writeToCSV("itemHasSeller.csv", itemID + DELIMITER + userID);
			//print to ItemHasSeller Table itemID, userID
		}

		/*
		 * here we check to seperate duplicate elements like Location and
		 * Country
		 */
		if (name.equals("Bid"))
			foundBid = true;

	}

	public void endElement (String uri, String name, String qName)
    {
	switch (name){	// I use switch instead of if-then-else because it is a bit more efficient in this case( plus it uses .equal() by default)
	case "Name":
		this.name = myString.toString();
		break;
	case "Category":
		String category = myString.toString();		
		/* Update Table Categories only if we have a new one */
		if ( addToCategory(category) ){
			int categoryID = categories.get(category);
			String row = String.valueOf(categoryID) + DELIMITER + category;
			writeToCSV("categories.csv", row);
		}
		/* Update Table ItemHasCategory */
		int categoryID = categories.get(category);
		String row = String.valueOf(itemID) + DELIMITER + categoryID;
		if (!innerCat.contains(row))
		writeToCSV("itemHasCategory.csv", row);		
		innerCat.add(row);
		break;
	case "Currently":
		currently = strip(myString.toString());
		break;
	case "Buy_Price":
		buyPrice = strip(myString.toString());
		break;
	case "First_Bid":
		firstBid = strip(myString.toString());
		break;
	case "Number_of_Bids":
		numberOfBids = myString.toString();
		break;
	case "Bid":						// there is no other element with that name so we don'y need further checks
		//print bidID,time,amount,bidder_id to BIDS table
		String bidsRow = bidID + DELIMITER + time + DELIMITER + strip(amount) + DELIMITER + bidderID;
		writeToCSV("bids.csv", bidsRow);
		
		String itemHasBidRow = itemID + DELIMITER + bidID;
		writeToCSV("itemHasBid.csv", itemHasBidRow);
		
		String biddersRow = bidderID + DELIMITER + bidderRating + DELIMITER + bidderLocation + DELIMITER + bidderCountry;
		if ( !bidders.contains(bidderID) ){	//Print only if its unique to avoid duplicated lines
			bidders.add(bidderID);
			writeToCSV("bidders.csv", biddersRow);
		}					
		bidID++;
		bidderLocation = "-1";
		bidderCountry = "-1";
		foundBid = false;
		break;
	case "Location":
		if( foundBid == true){				// to seperate which one is the correct Location
			bidderLocation = myString.toString();
		}else{
			location = myString.toString();
			String coord = latitude + DELIMITER + longitude;
			/* Update Table Coordinates only if we have a new one */
			if ( addToCoordinates(coord) ){
				int coordID = coordinates.get(coord);
				String myrow = String.valueOf(coordID) + DELIMITER + coord;
				writeToCSV("coordinates.csv", myrow);
			}
			/* Update Table ItemHasCoordinate */
			int coordID = coordinates.get(coord);
			String coordRow = String.valueOf(itemID) + DELIMITER + coordID;
			writeToCSV("itemHasCoordinate.csv", coordRow);	
			latitude = "-999";
			longitude = "-999";
		}
		break;
	case "Country":
		if( foundBid == true){				// to seperate which one is the correct Location
			bidderCountry = myString.toString();
		}else{
			country = myString.toString();
		}
		break;
	case "Time":						// there is no other element with that name so we don'y need further checks
		time =transformDate( myString.toString());
		break;
	case "Amount":						// the same for the "Amount" element
		amount = myString.toString();
		break;
	case "Started":
		started = transformDate( myString.toString() );
		break;
	case "Ends":
		ends = transformDate( myString.toString() );
		break;
	case "Description":
		description = myString.toString();
		break;
	case "Item":
		String itemRow = itemID + DELIMITER + this.name + DELIMITER + currently + DELIMITER + buyPrice + DELIMITER +
		firstBid + DELIMITER + numberOfBids + DELIMITER + location +DELIMITER + country + DELIMITER + 
		started + DELIMITER + ends + DELIMITER + description;
		writeToCSV("items.csv", itemRow);
		buyPrice = "-1";
		innerCat.clear(); // This is to improve memory usage we clear this inner set for every Item
		break;
	default:
		// We dont care about the other elements
		break;
	}

    }

	public void characters(char ch[], int start, int length) {
		StringBuilder temp = new StringBuilder();
		myString.append(ch, start, length);
	}
	/**
	 * If Category is new it is added to the HashMap otherwise we skip it
	 * @param catString
	 * @return
	 */
	public boolean addToCategory(String catString) {
		if (!categories.containsKey(catString)) {
			categories.put(catString, categoriesID);
			categoriesID++;
			return true;
		}
		return false;
	}
	/**
	 * If Coordinate is new it is added to the HashMap otherwise we skip it
	 * @param catString
	 * @return
	 */
	public boolean addToCoordinates(String catString) {
		if (!coordinates.containsKey(catString)) {
			coordinates.put(catString, coordinateID);
			coordinateID++;
			return true;
		}
		return false;
	}
	public void writeToCSV(String fileName, String row){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(fileName,true);
 			fileWriter.append(row);
			fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}
	public static void createCSVs(){
		createItems();
		createCategories();
		createItemHasCategory();
		createSellers();
		createItemHasSeller();
		createBids();
		createBidders();
		createItemHasBid();
		createCoordinates();
		createItemHasCoordinate();
	}
	/**
	 * Creation of Table Items ( items.csv )
	 */
	public static void createItems(){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("items.csv");
			String header = "itemID" + DELIMITER + "name" + DELIMITER + "currently" + DELIMITER + "buyPrice" + DELIMITER +
					"firstBid" + DELIMITER + "numberOfBids" + DELIMITER + "location" +DELIMITER + "country" + DELIMITER + 
					"started" + DELIMITER + "ends" + DELIMITER + "description";
			//fileWriter.append(header);
			//fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}	
	/**
	 * Creation of Table Categories ( categories.csv )
	 */
	public static void createCategories(){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("categories.csv");
			String header = "categoryID" + DELIMITER + "category";
			//fileWriter.append(header);
			//fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}	
	/**
	 * Creation of Table ItemHasCategory ( itemHasCategory.csv )
	 */
	public static void createItemHasCategory(){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("itemHasCategory.csv");
			String header = "itemID" + DELIMITER + "categoryID";
			//fileWriter.append(header);
			//fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}	
	/**
	 * Creation of Table Sellers ( sellers.csv )
	 */
	public static void createSellers(){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("sellers.csv");
			String header = "userID" + DELIMITER + "rating";
			//fileWriter.append(header);
			//fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}	
	/**
	 * Creation of Table ItemHasSeller ( itemHasSeller.csv )
	 */
	public static void createItemHasSeller(){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("itemHasSeller.csv");
			String header = "itemID" + DELIMITER + "userID";
			//fileWriter.append(header);
			//fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}	
	/**
	 * Creation of Table Bids ( bids.csv )
	 */
	public static void createBids(){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("bids.csv");
			String header = "bidID" + DELIMITER + "time" + DELIMITER + "amount" + DELIMITER + "bidderID";
			//fileWriter.append(header);
			//fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}
	/**
	 * Creation of Table ItemHasBid ( itemHasBid.csv )
	 */
	public static void createItemHasBid(){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("itemHasBid.csv");
			String header = "itemID" + DELIMITER + "bidID";
			//fileWriter.append(header);
			//fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}
	/**
	 * Creation of Table bidders ( bidders.csv )
	 */
	public static void createBidders(){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("bidders.csv");
			String header = "userID" + DELIMITER + "rating" + DELIMITER + "location" + DELIMITER + "country";
			//fileWriter.append(header);
			//fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}
	/**
	 * Creation of Table Coordinates ( coordinates.csv )
	 */
	public static void createCoordinates(){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("coordinates.csv");
			String header = "coordinateID" + DELIMITER + "latitude" + DELIMITER + "longitude";
			//fileWriter.append(header);
			//fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}
	/**
	 * Creation of Table ItemHasCoordinate ( itemHasCoordinate.csv )
	 */
	public static void createItemHasCoordinate(){
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter("itemHasCoordinate.csv");
			String header = "itemID" + DELIMITER + "coordinateID";
			//fileWriter.append(header);
			//fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (Exception e) {
			System.out.println("Error while writing to .csv file");
			e.printStackTrace();
		} finally{
			try {
				fileWriter.flush();
				fileWriter.close();
			} catch (IOException e) {
				System.out.println("Error when closing .csv file!");
				e.printStackTrace();
			}
			
		}
	}
}

