import java.io.StringReader;
import java.io.File;
import java.nio.file.*;
import java.lang.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Indexer {
	public Indexer() {
	}

	public static IndexWriter indexWriter = null;

	public static void main(String args[]) {
		String usage = "java Indexer";
		rebuildIndexes("indexes");
	}

	public static void insertDoc(String item_id, String item_name, String category, String desc) {

		try {
			Document doc = new Document();
			doc.add(new StringField("item_id", item_id, Field.Store.YES));
			doc.add(new TextField("item_name", item_name, Field.Store.YES));
			// doc.add(new TextField("category", category, Field.Store.NO));
			// doc.add(new TextField("description", desc, Field.Store.NO));

			String fullSearchableText = item_name + " " + category + " " + desc;
			doc.add(new TextField("content", fullSearchableText, Field.Store.NO));

			indexWriter.addDocument(doc);
		} catch (Exception ex) {
			System.err.println(ex);
		}
	}

	public static void rebuildIndexes(String indexPath) {

		Connection dbConnection = null;
		PreparedStatement preparedStatement = null;
		Directory directory = null;
		int max_rows = 0;
		try {
			Path path = Paths.get(indexPath);
			System.out.println("Indexing to directory '" + indexPath + "'...\n");
			directory = FSDirectory.open(path);

			// I will use SimpleAnalyzer which carries out case folding.
			IndexWriterConfig config = new IndexWriterConfig(new SimpleAnalyzer());
			indexWriter = new IndexWriter(directory, config);
			indexWriter.deleteAll();

			// now lets use JDBC to retrieve data from the Databsase
			// create a connection to the database to retrieve Items from MySQL
			dbConnection = DbManager.getConnection(true);

			// Here we just get the total rows to create an awesome progress bar
			// ! or we can hard code the number 19532
			max_rows = 19532;
			/*
			String forFunQuery = "SELECT COUNT(*) AS rowcount FROM item";
			PreparedStatement statementForFun = dbConnection.prepareStatement(forFunQuery);
			ResultSet r = statementForFun.executeQuery();
			r.next();
			max_rows = r.getInt("rowcount");
			r.close();
			*/
			
			String query = "SELECT "
					+ "item.item_id, item_name, description, GROUP_CONCAT(category_name SEPARATOR ' ') as category_name "
					+ "FROM item " + "INNER JOIN has_category ON item.item_id = has_category.item_id "
					+ "GROUP BY item.item_id";

			preparedStatement = dbConnection.prepareStatement(query);
			ResultSet rs = preparedStatement.executeQuery();

			double progressBarCounter = 0;

			// For every Item
			while (rs.next()) {

				int item_id = rs.getInt("item.item_id");
				String item_name = rs.getString("item_name");
				String description = rs.getString("description");
				String categories = rs.getString("category_name");

				// insert our data to the doc
				insertDoc(String.valueOf(item_id), item_name, categories, description);

				// update the progress bar
				progressBarCounter += 100.0 / max_rows;
				printProgBar((int) progressBarCounter);
			}
			printProgBar(100);
			System.out.println("");
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				// close Everything
				indexWriter.close();
				directory.close();
				preparedStatement.close();
				dbConnection.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	// Print a progress bar for fun
	public static void printProgBar(int percent) {
		StringBuilder bar = new StringBuilder("[");

		for (int i = 0; i < 50; i++) {
			if (i < (percent / 2)) {
				bar.append("=");
			} else if (i == (percent / 2)) {
				bar.append(">");
			} else {
				bar.append(" ");
			}
		}

		bar.append("]   " + percent + "%     ");
		System.out.print("\r" + bar.toString());
	}
}
