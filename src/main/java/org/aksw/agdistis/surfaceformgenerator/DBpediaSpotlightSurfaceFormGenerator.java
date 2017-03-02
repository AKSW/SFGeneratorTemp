package org.aksw.agdistis.surfaceformgenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

/**
 * @author Daniel Gerber <dgerber@informatik.uni-leipzig.de>
 */
public class DBpediaSpotlightSurfaceFormGenerator {

	private static final Logger logger = Logger.getLogger("DBpediaSpotlightSurfaceFormGenerator");

	private static final int MAXIMUM_SURFACE_FORM_LENGHT = 50;

	private static List<String> LOWERCASE_STOPWORDS = null;
	private static final List<String> STOPWORDS = Arrays.asList("but", "i", "a", "about", "an", "and", "are", "as", "at", "be", "by", "com", "for", "from", "how", "in", "is", "it", "of", "on", "or",
	        "that", "the", "this", "to", "what", "when", "where", "who", "will", "with", "the", "www", "before", ",", "after", ";", "like", "and", "such");

	private static Set<String> addNonAccentVersion(Set<String> surfaceForms) {

		// remove all the accents in the surface forms and add that new label
		Set<String> normalizedLabels = new HashSet<String>();
		for (String surfaceForm : surfaceForms) {

			String normalized = Normalizer.normalize(surfaceForm, Normalizer.Form.NFD);
			normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
			if (!normalized.equals(surfaceForm)) {
				normalizedLabels.add(normalized);
			}
		}
		surfaceForms.addAll(normalizedLabels);

		return surfaceForms;
	}

	private static void addSurfaceForm(Map<String, Set<String>> surfaceForms, String key, String value) {

		// clean and URL decode, whitespace removal
		String newSurfaceForm = createCleanSurfaceForm(value);
		if (newSurfaceForm != null) {

			if (surfaceForms.containsKey(key)) {
				surfaceForms.get(key)
				            .add(newSurfaceForm);
			} else {

				Set<String> sfList = new HashSet<String>();
				sfList.add(newSurfaceForm);
				surfaceForms.put(key, sfList);
			}
		}
	}

	private static void addSurfaceFormAnchor(Map<String, Set<String>> surfaceForms, String key, String value) {

		// clean and URL decode, whitespace removal
		String newSurfaceForm = value;
		if (newSurfaceForm != null) {

			if (surfaceForms.containsKey(key)) {
				surfaceForms.get(key)
				            .add(newSurfaceForm);
			} else {

				Set<String> sfList = new HashSet<String>();
				sfList.add(newSurfaceForm);
				surfaceForms.put(key, sfList);
			}
		}
	}

	private static String createCleanSurfaceForm(String label) {

		try {

			String newLabel = URLDecoder.decode(label, "UTF-8");
			newLabel = newLabel.replaceAll("_", " ")
			                   .replaceAll(" +", " ")
			                   .trim();
			newLabel = newLabel.replaceAll(" \\(.+?\\)$", "");

			return isGoodSurfaceForm(newLabel) ? newLabel : null;
		} catch (IllegalArgumentException e) {

			String error = "Could not decode label: " + label + " with URLDecoder.";
			System.out.println(error);
			return null;
		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
			String error = "Could not decode label: " + label + " with encoding UTF-8";
			System.out.println(error);
			throw new RuntimeException(error, e);
		}
	}

	private static boolean isGoodSurfaceForm(String surfaceForm) {

		if (surfaceForm.length() > MAXIMUM_SURFACE_FORM_LENGHT || surfaceForm.matches("^[\\W\\d]+$")) {

			logger.log(Level.FINEST, "Surfaceform: " + surfaceForm + " is not a good surface form because its too long or regex match.");
			return false;
		}

		int i = 0;
		for (String token : surfaceForm.toLowerCase()
		                               .split(" ")) {

			// current token is not a stopword
			if (!LOWERCASE_STOPWORDS.contains(token)) {
				i++;
			}
		}
		// at least one non stop word found
		if (i > 0) {
			return true;
		} else {

			logger.log(Level.FINEST, "Surfaceform: " + surfaceForm + " is not a good surface form because it contains only stop words.");
			return false;
		}
	}

	/**
	 *
	 * @param uri
	 * @return
	 */
	private static boolean isGoodUri(String uri) {

		if (uri.contains("Liste_") || uri.contains("(Begriffsklärung)") || uri.contains("List_of_") || uri.contains("(Disambiguation)") || uri.contains("/") || uri.contains("%23")
		        || uri.matches("^[\\W\\d]+$")) {

			logger.log(Level.FINEST, "Uri: <" + uri + "> is not a good uri! / or %23 or regex");
			return false;
		}
		return true;
	}

	/**
	 *
	 * @return @throws UnsupportedEncodingException
	 */
	private Set<String> createConceptUris() throws UnsupportedEncodingException {

		Set<String> conceptUris = new HashSet<String>();
		Set<String> badUris = new HashSet<String>();
		badUris.addAll(NtripleUtil.getSubjectsFromNTriple(SurfaceFormGenerator.DBPEDIA_REDIRECTS_FILE, ""));
		logger.info("Finished reading redirect file for bad uri detection!");
		badUris.addAll(NtripleUtil.getSubjectsFromNTriple(SurfaceFormGenerator.DBPEDIA_DISAMBIGUATIONS_FILE, ""));
		logger.info("Finished reading disambiguations file for bad uri detection!");

		// every uri which looks like a good uri and is not in the
		// disambiguations or redirect files is a concept uri
		// TODO check this

		Model model = ModelFactory.createMemModelMaker()
		                          .createModel(SurfaceFormGenerator.FILTERED_LABELS_FILE);
		StmtIterator statements = model.listStatements();
		while (statements.hasNext()) {

			Statement node = statements.next();
			String subjectUri = node.getSubject()
			                        .getURI();

			String subjectUriWihtoutPrefix = subjectUri.substring(subjectUri.lastIndexOf("/") + 1);

			if (isGoodUri(subjectUriWihtoutPrefix) && !badUris.contains(subjectUri)) {
				conceptUris.add(subjectUri);
			}
		}
		logger.info("Concept Uris construction complete! Total of: " + conceptUris.size() + " concept URIs found!");
		return conceptUris;
	}

	public Map<String, Set<String>> createOrReadSurfaceForms() throws IOException {
		System.out.println(SurfaceFormGenerator.SURFACE_FORMS_FILE);
		if (new File(SurfaceFormGenerator.SURFACE_FORMS_FILE).exists()) {
			return this.initializeSurfaceFormsFromFile();
		}

		initStopwords();
		Set<String> conceptUris = createConceptUris();
		Map<String, Set<String>> surfaceForms = new HashMap<String, Set<String>>();

		// first add all uris of the concept uris
		for (String uri : conceptUris) {
			addSurfaceForm(surfaceForms, uri, uri.substring(uri.lastIndexOf("/") + 1));
		}

		logger.info("Finished adding all conceptUris: " + surfaceForms.size());

		List<String[]> subjectToObject = NtripleUtil.getSubjectAndObjectsFromNTriple(SurfaceFormGenerator.DBPEDIA_DISAMBIGUATIONS_FILE, "");
		subjectToObject.addAll(NtripleUtil.getSubjectAndObjectsFromNTriple(SurfaceFormGenerator.DBPEDIA_REDIRECTS_FILE, ""));
		// subjectToObject.addAll(NtripleUtil.getSubjectAndObjectsFromNTriple(SurfaceFormGenerator.DBPEDIA_INTER_LANGUAGE_LINKS_FILE,
		// ""));

		// List<String[]> subjectToObject2 =
		// NtripleUtil.getSubjectAndObjectsFromNTriple(SurfaceFormGenerator.ANCHOR_FILE,
		// "");
		List<String[]> subjectToObject2 = NtripleUtil.getSubjectAndObjectsFromNTriple(SurfaceFormGenerator.DBPEDIA_INTER_LANGUAGE_LINKS_FILE, "");

		// subjectToObject.addAll(NtripleUtil.getSubjectAndObjectsFromNTriple(SurfaceFormGenerator.ANCHOR_FILE,
		// ""));
		for (String[] subjectAndObject : subjectToObject) {

			String subject = subjectAndObject[0];
			String object = subjectAndObject[1];

			if (conceptUris.contains(object) && !object.contains("%")) {
				addSurfaceForm(surfaceForms, object, subject.substring(subject.lastIndexOf("/") + 1));
			}
		}

		for (String[] subjectAndObject : subjectToObject2) {

			String subject = subjectAndObject[0];
			String object = subjectAndObject[1];

			String tempSubject = subject.substring(subject.lastIndexOf("/") + 1);
			String tempObject = object.substring(object.lastIndexOf("/") + 1);

			if (!tempSubject.equals(tempObject)) {
				if (conceptUris.contains(subject) && !subject.contains("%")) {
					addSurfaceFormAnchor(surfaceForms, subject, object);
				}
			}
		}

		logger.info("Finished generation of surface forms.");

		// write the file
		// TODO auf streamwriter mit encoding umschreiben
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(SurfaceFormGenerator.SURFACE_FORMS_FILE, false), "UTF-8");

		for (Map.Entry<String, Set<String>> entry : surfaceForms.entrySet()) {
			writer.write(entry.getKey() + "\t" + StringUtils.join(addNonAccentVersion(entry.getValue()), "\t"));
		}

		writer.close();
		logger.info("Finished writing of surface forms to disk.");

		return surfaceForms;
	}

	private Map<String, Set<String>> initializeSurfaceFormsFromFile() {
		logger.info("Intializing surface forms from file...");
		Map<String, Set<String>> urisToLabels = new HashMap<String, Set<String>>();

		// initialize the surface forms from dbpedia spotlight
		try (TSVReader tsvReader = new TSVReader(new File(SurfaceFormGenerator.SURFACE_FORMS_FILE));) {
			String[] tokens;
			String uri;
			Set<String> filteredSurfaceForms;
			while (tsvReader.hasNextTokens()) {
				tokens = tsvReader.nextTokens();
				uri = tokens[0];
				filteredSurfaceForms = new HashSet<String>();
				String surfaceForm;
				for (int i = 1; i < tokens.length; i++) {
					surfaceForm = tokens[i];
					if (surfaceForm.length() <= MAXIMUM_SURFACE_FORM_LENGHT) {
						filteredSurfaceForms.add(surfaceForm);
					}
				}
				urisToLabels.put(uri.replace("http://en.", "http://")
				                    .replace("http://dbpedia.org/resource/", ""), filteredSurfaceForms);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Finished intializing surface forms! Found " + urisToLabels.size() + " dbpedia spotlight surfaceforms in file");
		// for(String uri:urisToLabels.keySet()) ...
		// "http://dbpedia.org/resource/"
		return urisToLabels;
	}

	private void initStopwords() {

		List<String> lowerCaseStopWords = new ArrayList<String>();
		for (String stopword : STOPWORDS) {

			lowerCaseStopWords.add(stopword.toLowerCase());
		}
		LOWERCASE_STOPWORDS = lowerCaseStopWords;
		logger.info("There were " + LOWERCASE_STOPWORDS.size() + " lowercase stopwords found.");
	}
}
