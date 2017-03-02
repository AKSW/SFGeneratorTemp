package org.aksw.agdistis.surfaceformgenerator;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NtripleUtil {

	private static final Logger logger = LoggerFactory.getLogger(NtripleUtil.class);

	public static List<String[]> getSubjectAndObjectsFromNTriple(String filename, String replacePrefix) {

		List<String[]> results = new ArrayList<String[]>();

		Model model = ModelFactory.createMemModelMaker()
		                          .createModel(filename);
		StmtIterator statements = model.listStatements();
		while (statements.hasNext()) {

			Statement statement = statements.next();

			results.add(new String[] { replacePrefix == null || replacePrefix.equals("") ? statement.getSubject()
			                                                                                        .getURI() : statement.getSubject()
			                                                                                                             .getURI()
			                                                                                                             .replace(replacePrefix, ""),
			        replacePrefix == null || replacePrefix.equals("") ? statement.getObject()
			                                                                     .asResource()
			                                                                     .getURI() : statement.getObject()
			                                                                                          .asResource()
			                                                                                          .getURI()
			                                                                                          .replace(replacePrefix, ""), });
		}
		return results;
	}

	public static List<String> getSubjectsFromNTriple(String filename, String replacePrefix) {

		List<String> results = new ArrayList<String>();

		Model model = ModelFactory.createMemModelMaker()
		                          .createModel(filename);
		StmtIterator statements = model.listStatements();
		while (statements.hasNext()) {

			Statement statement = statements.next();
			results.add(replacePrefix == null || replacePrefix.equals("") ? statement.getSubject()
			                                                                         .getURI() : statement.getSubject()
			                                                                                              .getURI()
			                                                                                              .replace(replacePrefix, ""));
		}
		return results;
	}

}