package org.aksw.agdistis.surfaceformgenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Gerber <dgerber@informatik.uni-leipzig.de>
 * @author Diego Moussallem
 * @author Ricardo Usbeck <usbeck@informatik.uni-leipzig.de>
 */
public class IndexDocument {

	private String uri = "";
	private String label = "";
	private String imageUri = "";
	private Set<String> types = new HashSet<String>();
	private Set<String> surfaceForms = new HashSet<String>();
	private Integer pagerank = 0;
	private Double disambiguationScore = 0d;
	private String shortAbstract = "";
	private String dbpediaUri = "";

	/**
	 * @return the dbpediaUri
	 */
	public String getCanonicalDBpediaUri() {
		return dbpediaUri;
	}

	public Double getDisambiguationScore() {
		return disambiguationScore;
	}

	/**
	 * @return the imageUri
	 */
	public String getImageUri() {

		return imageUri;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {

		return label;
	}

	/**
	 * @return the pagerank
	 */
	public int getPageRank() {

		return pagerank;
	}

	/**
	 * @return the shortAbstract
	 */
	public String getShortAbstract() {

		return shortAbstract;
	}

	/**
	 * @return the surfaceForms
	 */
	public Set<String> getSurfaceForms() {

		return surfaceForms;
	}

	/**
	 * @return the types
	 */
	public Set<String> getTypes() {

		return types;
	}

	/**
	 * @return the uri
	 */
	public String getUri() {

		return uri;
	}

	public void setCanonicalDBpediaUri(String uri) {

		this.dbpediaUri = uri;
	}

	public void setDisambiguationScore(Double disambiguationScore) {
		this.disambiguationScore = disambiguationScore;
	}

	/**
	 * @param imageUri
	 *            the imageUri to set
	 */
	public void setImageUri(String imageUri) {

		this.imageUri = imageUri;
	}

	/**
	 * @param label
	 *            the label to set
	 */
	public void setLabel(String label) {

		this.label = label;
	}

	/**
	 * @param pagerank
	 *            the pagerank to set
	 */
	public void setPageRank(int pagerank) {

		this.pagerank = pagerank;
	}

	/**
	 * @param shortAbstract
	 *            the shortAbstract to set
	 */
	public void setShortAbstract(String shortAbstract) {

		this.shortAbstract = shortAbstract;
	}

	/**
	 * @param surfaceForms
	 *            the surfaceForms to set
	 */
	public void setSurfaceForms(Set<String> surfaceForms) {

		this.surfaceForms = surfaceForms;
	}

	/**
	 * @param types
	 *            the types to set
	 */
	public void setTypes(Set<String> types) {

		this.types = types;
	}

	/**
	 * @param uri
	 *            the uri to set
	 */
	public void setUri(String uri) {

		this.uri = uri;
	}
}
