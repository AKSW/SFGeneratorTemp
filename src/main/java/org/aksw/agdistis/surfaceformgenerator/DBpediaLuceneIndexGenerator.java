package org.aksw.agdistis.surfaceformgenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ext.com.google.common.net.UrlEscapers;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

/**
 * @author Daniel Gerber <dgerber@informatik.uni-leipzig.de>
 * @author Diego Moussallem
 * @author Ricardo Usbeck <usbeck@informatik.uni-leipzig.de>
 */
public class DBpediaLuceneIndexGenerator {

	private class NoLabelException extends Exception {
		public NoLabelException(String uri) {
			super("No label found for uri " + uri);
		}
	}

	/**
	 * 
	 */
	private static final String DBPEDIA_NAMESPACE = "http://dbpedia.org/resource/";
	private static final int BATCH_SIZE = 10000;
	private static final int maxNrOfTries = 10;
	private static final int DELAY_IN_MS = 5;
	// default values, should get overwritten with main args
	private static String GRAPH = "http://dbpedia.org";
	private static double RAM_BUFFER_MAX_SIZE = 14;
	private static boolean OVERWRITE_INDEX = true;
	private static Boolean FILTER_SURFACE_FORMS = true;
	public static String DIRECTORY = "/Users/diegomoussallem/NetBeansProjects/SF/data/2016-04/de/";
	public static String LANGUAGE = "de";
	private static String INDEX_DIRECTORY = "2015-10/en/";
	private static String SPARQL_ENDPOINT = "http://dbpedia.org/sparql";

	private static IndexWriter writer;
	public static String DBPEDIA_REDIRECTS_FILE = "" + DIRECTORY + "redirects_" + LANGUAGE + ".ttl";
	public static String DBPEDIA_LABELS_FILE = "" + DIRECTORY + "labels_" + LANGUAGE + ".ttl";
	public static String DBPEDIA_DISAMBIGUATIONS_FILE = "" + DIRECTORY + "disambiguations_" + LANGUAGE + ".ttl";
	public static String SURFACE_FORMS_FILE = "" + DIRECTORY + "de_surface_forms.tsv";
	public static String FILTERED_LABELS_FILE = "" + DIRECTORY + "labels_de_filtered.ttl";

	// public static String INTER_LANGUAGE_LINKS_jrcFILE =
	// "/Users/diegomoussallem/NetBeansProjects/SurfaceForm/2015-10/en/jrcNamesInterlink.ttl";
	// public static String jrcNames_FILE =
	// "/Users/diegomoussallem/NetBeansProjects/SurfaceForm/2015-10/en/jrcNames.ttl";
	// public static String ANCHOR_FILE = ""+DIRECTORY+"anchor_text_en.ttl";

	public static String INTER_LANGUAGE_LINKS_FILE = "" + DIRECTORY + "en/interlanguage_links_e" + LANGUAGE + ".ttl";

	private static Map<String, String> createInterLanguageLinks() {

		Map<String, String> languageToDbpediaUris = new HashMap<String, String>();

		if (LANGUAGE.equals("en"))
			return languageToDbpediaUris;

		Model model = ModelFactory.createMemModelMaker()
		                          .createModel(DBpediaLuceneIndexGenerator.INTER_LANGUAGE_LINKS_FILE);
		StmtIterator statements = model.listStatements();
		while (statements.hasNext()) {

			Statement statement = statements.next();

			String subjectUri = statement.getSubject()
			                             .getURI();
			String objectUri = statement.getObject()
			                            .asResource()
			                            .getURI();

			if (objectUri.startsWith("http://dbpedia.org"))
				languageToDbpediaUris.put(subjectUri, objectUri);
		}

		return languageToDbpediaUris;
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws CorruptIndexException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws CorruptIndexException, IOException, InterruptedException {
		System.out.println(UrlEscapers.urlFragmentEscaper()
		                              .escape("http://dbpedia.org/resource/Miłkowice,_Greater_Poland_Voivodeship"));
		for (int i = 0; i < args.length; i = i + 2) {

			if (args[i].equals("-o"))
				OVERWRITE_INDEX = Boolean.valueOf(args[i + 1]);
			if (args[i].equals("-b"))
				RAM_BUFFER_MAX_SIZE = Double.valueOf(args[i + 1]);
			if (args[i].equals("-d"))
				DIRECTORY = args[i + 1];
			if (args[i].equals("-i"))
				INDEX_DIRECTORY = args[i + 1];
			if (args[i].equals("-s"))
				SPARQL_ENDPOINT = args[i + 1];
			if (args[i].equals("-g"))
				GRAPH = args[i + 1];
			if (args[i].equals("-l"))
				LANGUAGE = args[i + 1];
			if (args[i].equals("-f"))
				FILTER_SURFACE_FORMS = new Boolean(args[i + 1]);

			DBPEDIA_REDIRECTS_FILE = DBpediaLuceneIndexGenerator.DIRECTORY + "redirects_" + LANGUAGE + ".ttl";
			DBPEDIA_LABELS_FILE = DBpediaLuceneIndexGenerator.DIRECTORY + "labels_" + LANGUAGE + ".ttl";
			DBPEDIA_DISAMBIGUATIONS_FILE = DBpediaLuceneIndexGenerator.DIRECTORY + "disambiguations_" + LANGUAGE + ".ttl";
			SURFACE_FORMS_FILE = DBpediaLuceneIndexGenerator.DIRECTORY + LANGUAGE + "_surface_forms.tsv";
			FILTERED_LABELS_FILE = DBpediaLuceneIndexGenerator.DIRECTORY + "labels_" + LANGUAGE + "_filtered.ttl";
			INTER_LANGUAGE_LINKS_FILE = DBpediaLuceneIndexGenerator.DIRECTORY + "interlanguage_links_" + LANGUAGE + ".ttl";
		}

		DBpediaSpotlightSurfaceFormGenerator surfaceFormGenerator = new DBpediaSpotlightSurfaceFormGenerator();

		// we need to break here, because after the step we need to import the
		// stuff to virtuoso
		if (FILTER_SURFACE_FORMS) {

			System.out.println("Starting to filter labels_" + LANGUAGE + ".uri!");
			Set<String> badUris = new HashSet<String>();
			badUris.addAll(NtripleUtil.getSubjectsFromNTriple(DBPEDIA_REDIRECTS_FILE, ""));
			System.out.println("Finished reading bad redirect uris!");
			badUris.addAll(NtripleUtil.getSubjectsFromNTriple(DBPEDIA_DISAMBIGUATIONS_FILE, ""));
			System.out.println("Finished reading bad disambiguations uris!");

			// write the file
			BufferedWriter writer = new BufferedWriter(new FileWriter(FILTERED_LABELS_FILE, false));
			System.out.println("Writing filtered labels file: " + FILTERED_LABELS_FILE);
			Model model = ModelFactory.createMemModelMaker()
			                          .createModel(DBPEDIA_LABELS_FILE);
			StmtIterator statements = model.listStatements();
			while (statements.hasNext()) {

				Statement statement = statements.next();

				String subjectUri = statement.getSubject()
				                             .getURI();

				if (!badUris.contains(subjectUri)) {
					// TODO check on correctnes
					writer.write(statement.asTriple()
					                      .toString());
				}
			}

			writer.close();

			// generate the surface forms (and save them to the file) or load
			// them from a file
			surfaceFormGenerator.createOrReadSurfaceForms();

			return;
		}

		System.out.println("Override-Index: " + OVERWRITE_INDEX);
		System.out.println("RAM-Buffer-Max-Size: " + RAM_BUFFER_MAX_SIZE);
		System.out.println("Index-Directory: " + INDEX_DIRECTORY);
		System.out.println("SPARQL-Endpoint: " + SPARQL_ENDPOINT);
		System.out.println("GRAPH: " + GRAPH);

		DBpediaLuceneIndexGenerator indexGenerator = new DBpediaLuceneIndexGenerator();

		// create the index writer configuration and create a new index writer
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_4_9, new StandardAnalyzer(Version.LUCENE_4_9));
		indexWriterConfig.setRAMBufferSizeMB(RAM_BUFFER_MAX_SIZE);
		indexWriterConfig.setOpenMode(OVERWRITE_INDEX || !indexGenerator.isIndexExisting(INDEX_DIRECTORY) ? OpenMode.CREATE : OpenMode.APPEND);
		writer = indexGenerator.createIndex(INDEX_DIRECTORY, indexWriterConfig);

		Map<String, Set<String>> surfaceForms = surfaceFormGenerator.createOrReadSurfaceForms();
		Map<String, String> language2dbpediaLinks = createInterLanguageLinks();

		Set<IndexDocument> indexDocuments = new HashSet<IndexDocument>((int) (BATCH_SIZE * 1.4));

		// time measurements
		int counter = 0;
		int noLabelCounter = 0;
		int http404Counter = 0;
		long start = System.currentTimeMillis();
		int total = surfaceForms.size();
		Set<String> surfaceFormValues;

		Iterator<Map.Entry<String, Set<String>>> surfaceFormIterator = surfaceForms.entrySet()
		                                                                           .iterator();
		while (surfaceFormIterator.hasNext()) {

			Map.Entry<String, Set<String>> entry = surfaceFormIterator.next();
			String fragment = entry.getKey();
			String uri = UrlEscapers.urlFragmentEscaper()
			                        .escape(DBPEDIA_NAMESPACE + fragment);
			try {
				long startTime = System.currentTimeMillis();
				indexDocuments.add(indexGenerator.queryAttributesForUri(uri, entry.getValue(), language2dbpediaLinks));
				long endTime = System.currentTimeMillis();
				// System.out.println("Operation took " + (endTime - startTime)
				// + "ms");
			} catch (NoLabelException e) {
				if (noLabelCounter++ % 100 == 0) {
					System.out.println(e.getMessage() + "\t|\t" + noLabelCounter + "/" + (counter + 1) + " labels faulty (" + 100 * noLabelCounter / counter + "%)");
				}
			}
			// don't know which one it is
			// catch(org.openjena.atlas.web.HttpException|org.apache.jena.atlas.web.HttpException
			// e)
			catch (QueryExceptionHTTP e) {
				System.out.println("Waiting for 1 second" + e.getMessage());
				Thread.sleep(1000);
			}

			// improve speed through batch save
			if (++counter % BATCH_SIZE == 0) {

				indexGenerator.addIndexDocuments(indexDocuments);
				System.out.println("Done: " + counter + "/" + total + " " + MessageFormat.format("{0,number,#.##%}", (double) counter / (double) total) + " in " + (System.currentTimeMillis() - start)
				        + "ms");
				start = System.currentTimeMillis();
				indexDocuments = new HashSet<IndexDocument>((int) (BATCH_SIZE * 1.4));

			}

		}
		// write the last few items
		indexGenerator.addIndexDocuments(indexDocuments);
		writer.commit();
		writer.close();
	}

	/**
	 * Adds a set of index documents in batch mode to the index Uris and image
	 * urls as well as type Uris are not analyzed. Termvectors are not stored
	 * for anything. Everything else is analyzed using a standard analyzer.
	 * 
	 * @param indexDocuments
	 *            - the documents to be indexed
	 * @throws CorruptIndexException
	 *             - index corrupted
	 * @throws IOException
	 *             - error
	 */
	private void addIndexDocuments(Set<IndexDocument> indexDocuments) throws CorruptIndexException, IOException {

		Set<Document> luceneDocuments = new HashSet<Document>();
		FieldType stringType = new FieldType(StringField.TYPE_STORED);
		stringType.setStoreTermVectors(false);
		FieldType textType = new FieldType(TextField.TYPE_STORED);
		textType.setStoreTermVectors(false);
		Document luceneDocument;
		for (IndexDocument indexDocument : indexDocuments) {

			luceneDocument = new Document();
			luceneDocument.add(new Field("uri", indexDocument.getUri(), stringType));
			luceneDocument.add(new Field("dbpediaUri", indexDocument.getCanonicalDBpediaUri(), stringType));
			luceneDocument.add(new Field("label", indexDocument.getLabel(), textType));
			luceneDocument.add(new Field("comment", indexDocument.getShortAbstract(), textType));
			luceneDocument.add(new Field("imageURL", indexDocument.getImageUri(), stringType));
			luceneDocument.add(new IntField("pagerank", indexDocument.getPageRank(), Field.Store.YES));
			luceneDocument.add(new DoubleField("disambiguationScore", indexDocument.getDisambiguationScore(), Field.Store.YES));
			for (String type : indexDocument.getTypes())
				luceneDocument.add(new Field("types", type, stringType));
			for (String surfaceForm : indexDocument.getSurfaceForms())
				luceneDocument.add(new Field("surfaceForms", surfaceForm, textType));

			luceneDocuments.add(luceneDocument);
		}
		writer.addDocuments(luceneDocuments);
	}

	/**
	 * Create a new filesystem lucene index
	 * 
	 * @param absoluteFilePath
	 *            - the path where to create/append the index
	 * @param indexWriterConfig
	 *            - the index write configuration
	 * @return
	 */
	private IndexWriter createIndex(String absoluteFilePath, IndexWriterConfig indexWriterConfig) {

		try {

			return new IndexWriter(FSDirectory.open(new File(absoluteFilePath)), indexWriterConfig);
		} catch (CorruptIndexException e) {

			e.printStackTrace();
			throw new RuntimeException("Could not create index", e);
		} catch (LockObtainFailedException e) {

			e.printStackTrace();
			throw new RuntimeException("Could not create index", e);
		} catch (IOException e) {

			e.printStackTrace();
			throw new RuntimeException("Could not create index", e);
		}
	}

	public double getAprioriScore1(String uri, String endpoint, String graph) {

		String query = "SELECT (COUNT(?s) AS ?cnt) WHERE {?s ?p <" + uri + ">}";
		Query sparqlQuery = QueryFactory.create(query);
		QueryExecution qexec;
		if (graph != null) {
			qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery, graph);
		} else {
			qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
		}
		ResultSet results = qexec.execSelect();
		int count = 0;
		try {
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				count = soln.getLiteral("cnt")
				            .getInt();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// logger.info(uri+" -> "+Math.log(count+1));
		return Math.log(count + 1);
	}

	/**
	 * Checks if an index exists at the given location.
	 * 
	 * @param indexDirectory
	 *            - the directory of the index to be checked
	 * @return true if the index exists, false otherwise
	 */
	public boolean isIndexExisting(String indexDirectory) {

		try {

			return DirectoryReader.indexExists(FSDirectory.open(new File(indexDirectory)));
		} catch (IOException e) {

			e.printStackTrace();
			String error = "Check if index exists failed!";
			throw new RuntimeException(error, e);
		}
	}

	/**
	 * Queries a configured sparql endpoint for all information a document needs
	 * - rank - label - uri - imageUrl - abstract - types
	 * 
	 * @param uri
	 *            the URI of the resource
	 * @param surfaceForms
	 *            the surface forms of this resource
	 * @param language2dbpediaLinks
	 * @return a document ready to be indexed
	 * @throws UnsupportedEncodingException
	 * @throws NoLabelException
	 */
	private IndexDocument queryAttributesForUri(String uri, Set<String> surfaceForms, Map<String, String> language2dbpediaLinks) throws UnsupportedEncodingException, NoLabelException {

		String query = String.format("SELECT (<LONG::IRI_RANK> (<%s>)) as ?rank ?label ?imageUrl ?abstract ?types " + "FROM <%s> " + "WHERE { "
		        + "   OPTIONAL { <%s> <http://www.w3.org/2000/01/rdf-schema#label> ?label . } " + "   OPTIONAL { <%s> <http://dbpedia.org/ontology/thumbnail> ?imageUrl . } "
		        + "   OPTIONAL { <%s> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?types . } " + "   OPTIONAL { <%s> <http://www.w3.org/2000/01/rdf-schema#comment> ?abstract . } " + "}", uri,
		        GRAPH, uri, uri, uri, uri);

		// execute the query
		IndexDocument document = new IndexDocument();
		QueryEngineHTTP qexec = new QueryEngineHTTP(SPARQL_ENDPOINT, query);
		int nrOfTries = 0;
		ResultSet result = null;
		while (result == null && nrOfTries++ <= maxNrOfTries) {
			try {
				result = qexec.execSelect();
			} catch (Exception e1) {
				System.err.println("An error occured while executing SPARQL query\n" + query + "\nRetrying...");
				e1.printStackTrace();
				try {
					Thread.sleep(DELAY_IN_MS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		qexec.close();

		if (result == null) {
			return null;
		}
		while (result.hasNext()) {

			QuerySolution solution = result.next();

			// those values do get repeated, we need them to set only one time
			if (document.getUri()
			            .isEmpty()) {

				document.setUri(URLDecoder.decode(uri, "UTF-8"));
				RDFNode label = solution.get("label");
				if (label == null)
					throw new NoLabelException(uri);
				document.setLabel(label.asLiteral()
				                       .getLexicalForm());

				document.setPageRank(solution.get("rank") != null ? Integer.valueOf(solution.get("rank")
				                                                                            .toString()
				                                                                            .replace("^^http://www.w3.org/2001/XMLSchema#integer", "")) : 0);
				document.setImageUri(solution.get("imageUrl") != null ? solution.get("imageUrl")
				                                                                .toString() : "");
				document.setShortAbstract(solution.get("abstract") != null ? solution.get("abstract")
				                                                                     .asLiteral()
				                                                                     .getLexicalForm() : "");
				document.setCanonicalDBpediaUri(language2dbpediaLinks.containsKey(uri) ? language2dbpediaLinks.get(uri) : "");

				try {
					double disambiguationScore = getAprioriScore1(uri, SPARQL_ENDPOINT, GRAPH);
					document.setDisambiguationScore(disambiguationScore);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			// there might be different types
			if (solution.get("types") != null)
				document.getTypes()
				        .add(solution.get("types")
				                     .toString());
		}
		document.setSurfaceForms(surfaceForms == null ? new HashSet<String>() : surfaceForms);

		return document;
	}
}
