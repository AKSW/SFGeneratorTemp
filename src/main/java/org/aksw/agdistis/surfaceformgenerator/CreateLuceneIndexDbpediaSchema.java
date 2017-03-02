package org.aksw.agdistis.surfaceformgenerator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * @author Daniel Gerber <dgerber@informatik.uni-leipzig.de>
 * @author Diego Moussallem
 * @author Ricardo Usbeck <usbeck@informatik.uni-leipzig.de>
 */
public class CreateLuceneIndexDbpediaSchema {
	// FIXME we will assume we can work on an owl file directly
	private static final String DBPEDIA_VERSION = "2016-04";
	private static final String DBPEDIA_SCHEMA_DOWNLOAD_URL = "http://downloads.dbpedia.org/" + DBPEDIA_VERSION + "/dbpedia_" + DBPEDIA_VERSION + ".owl";

	static IndexWriter createWriter(String filename) throws IOException {
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_4_9, new StandardAnalyzer(Version.LUCENE_4_9));
		indexWriterConfig.setRAMBufferSizeMB(100);
		indexWriterConfig.setOpenMode(OpenMode.CREATE);
		return new IndexWriter(FSDirectory.open(new File("output/" + filename)), indexWriterConfig);
	}

	public static void main(String[] args) throws IOException {
		Model model = ModelFactory.createDefaultModel();

		try (InputStream is = new URL(DBPEDIA_SCHEMA_DOWNLOAD_URL).openStream()) {
			model.read(is, null, "RDF/XML");
		}

		System.out.println(model.size() + " triples loaded.");

		Set<Resource> classes = subjects(model, RDF.type, OWL.Class);
		Set<Resource> objectProperties = subjects(model, RDF.type, OWL.ObjectProperty);
		Set<Resource> dataProperties = subjects(model, RDF.type, OWL.DatatypeProperty);

		Map<Set<Resource>, String> setToName = new HashMap<>();
		setToName.put(classes, "classes");
		setToName.put(objectProperties, "objectproperties");
		setToName.put(dataProperties, "dataproperties");

		FieldType stringType = new FieldType(StringField.TYPE_STORED);
		stringType.setStoreTermVectors(false);
		FieldType textType = new FieldType(TextField.TYPE_STORED);
		textType.setStoreTermVectors(false);

		for (Set<Resource> set : setToName.keySet()) {
			IndexWriter writer = createWriter(setToName.get(set));
			Set<Document> documents = new HashSet<>();

			for (Resource resource : set) {
				for (RDFNode object : model.listObjectsOfProperty(resource, RDFS.label)
				                           .toSet()) {
					String label = object.asLiteral()
					                     .getLexicalForm();

					Document luceneDocument = new Document();
					luceneDocument.add(new Field("uri", resource.getURI(), stringType));
					luceneDocument.add(new Field("label", label, textType));
					writer.addDocument(luceneDocument);
				}

			}
			writer.addDocuments(documents);
			writer.commit();
			writer.close();
		}

	}

	static Set<Resource> subjects(Model model, Property p, RDFNode r) {
		Set<Resource> subjects = new HashSet<>();
		ResIterator it = model.listSubjectsWithProperty(p, r);
		while (it.hasNext())
			subjects.add(it.next()
			               .asResource());
		it.close();
		return subjects;
	}
}