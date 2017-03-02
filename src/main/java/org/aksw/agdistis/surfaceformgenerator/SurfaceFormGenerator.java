package org.aksw.agdistis.surfaceformgenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.ext.com.google.common.net.UrlEscapers;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;

/**
 * @author Daniel Gerber <dgerber@informatik.uni-leipzig.de>
 * @author Diego Moussallem
 * @author Ricardo Usbeck
 */
public class SurfaceFormGenerator {

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
	public static String DIRECTORY = "data/2016-04/en/";
	public static String LANGUAGE = "en";
	private static String INDEX_DIRECTORY = "2016-04/en/";
	private static String SPARQL_ENDPOINT = "http://dbpedia.org/sparql";
	private static IndexWriter writer;

	public static String DBPEDIA_REDIRECTS_FILE = "" + DIRECTORY + "redirects_" + LANGUAGE + ".ttl";
	public static String DBPEDIA_LABELS_FILE = "" + DIRECTORY + "labels_" + LANGUAGE + ".ttl";
	public static String DBPEDIA_DISAMBIGUATIONS_FILE = "" + DIRECTORY + "disambiguations_" + LANGUAGE + ".ttl";
	public static String SURFACE_FORMS_FILE = "" + DIRECTORY + LANGUAGE + "_surface_forms.tsv";
	public static String FILTERED_LABELS_FILE = "" + DIRECTORY + "labels_" + LANGUAGE + "_filtered.ttl";
	public static String DBPEDIA_INTER_LANGUAGE_LINKS_FILE = "" + DIRECTORY + "interlanguage_links_" + LANGUAGE + ".ttl";

	// public static String INTER_LANGUAGE_LINKS_jrcFILE =
	// "/Users/diegomoussallem/NetBeansProjects/SurfaceForm/2015-10/en/jrcNamesInterlink.ttl";
	// public static String jrcNames_FILE =
	// "/Users/diegomoussallem/NetBeansProjects/SurfaceForm/2015-10/en/jrcNames.ttl";
	// public static String ANCHOR_FILE = ""+DIRECTORY+"anchor_text_en.ttl";

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

			if (args[i].equals("-o")) {
				OVERWRITE_INDEX = Boolean.valueOf(args[i + 1]);
			}
			if (args[i].equals("-b")) {
				RAM_BUFFER_MAX_SIZE = Double.valueOf(args[i + 1]);
			}
			if (args[i].equals("-d")) {
				DIRECTORY = args[i + 1];
			}
			if (args[i].equals("-i")) {
				INDEX_DIRECTORY = args[i + 1];
			}
			if (args[i].equals("-s")) {
				SPARQL_ENDPOINT = args[i + 1];
			}
			if (args[i].equals("-g")) {
				GRAPH = args[i + 1];
			}
			if (args[i].equals("-l")) {
				LANGUAGE = args[i + 1];
			}
			if (args[i].equals("-f")) {
				FILTER_SURFACE_FORMS = new Boolean(args[i + 1]);
			}

			DBPEDIA_REDIRECTS_FILE = DIRECTORY + "redirects_" + LANGUAGE + ".ttl";
			DBPEDIA_LABELS_FILE = DIRECTORY + "labels_" + LANGUAGE + ".ttl";
			DBPEDIA_DISAMBIGUATIONS_FILE = DIRECTORY + "disambiguations_" + LANGUAGE + ".ttl";
			SURFACE_FORMS_FILE = DIRECTORY + LANGUAGE + "_surface_forms.tsv";
			FILTERED_LABELS_FILE = DIRECTORY + "labels_" + LANGUAGE + "_filtered.ttl";
			DBPEDIA_INTER_LANGUAGE_LINKS_FILE = DIRECTORY + "interlanguage_links_" + LANGUAGE + ".ttl";
			// INTER_LANGUAGE_LINKS_jrcFILE =
			// DBpediaLuceneIndexGenerator.DIRECTORY + "jrcNamesInterlink";
			// jrcNames_FILE = DBpediaLuceneIndexGenerator.DIRECTORY +
			// "jrcNames.ttl";
			// ANCHOR_FILE = DBpediaLuceneIndexGenerator.DIRECTORY +
			// "anchor_text_" + LANGUAGE + ".ttl";

		}
		DBpediaSpotlightSurfaceFormGenerator surfaceFormGenerator = new DBpediaSpotlightSurfaceFormGenerator();

		// we need to break here, because after the step we need to import the
		// stuff to virtuoso

		if (FILTER_SURFACE_FORMS) {

			// System.out.println("Starting to filter labels_" + LANGUAGE +
			// ".uri!");
			Set<String> badUris = new HashSet<String>();
			badUris.addAll(NtripleUtil.getSubjectsFromNTriple(DBPEDIA_REDIRECTS_FILE, ""));
			System.out.println("Finished reading bad redirect uris!");
			badUris.addAll(NtripleUtil.getSubjectsFromNTriple(DBPEDIA_DISAMBIGUATIONS_FILE, ""));
			System.out.println("Finished reading bad disambiguations uris!");
			// badUris.addAll(NtripleUtil.getSubjectsFromNTriple(DBPEDIA_INTER_LANGUAGE_LINKS_FILE,
			// ""));
			// System.out.println("Finished reading bad interlinks uris!");
			// badUris.addAll(NtripleUtil.getSubjectsFromNTriple(ANCHOR_FILE,
			// ""));
			// System.out.println("Finished reading bad anchor uris!");

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
					// TODO test if this is valid RDF
					writer.write(statement.asTriple()
					                      .toString());
				}
			}

			writer.close();

			// generate the surface forms (and save them to the file) or load
			// them from a file
			surfaceFormGenerator.createOrReadSurfaceForms();
		} else {

			// // generate the surface forms (and save them to the file) or load
			// them from a file
			surfaceFormGenerator.createOrReadSurfaceForms();
		}

	}

}
