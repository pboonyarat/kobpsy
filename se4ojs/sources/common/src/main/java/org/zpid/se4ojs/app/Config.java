package org.zpid.se4ojs.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Reads the configuration properties from a file.
 * This class should be used as a Singleton (only supposed to be overwritten for testing purposes).
 *
 * @author barth
 *
 */
public class Config {

	public static final String DEFAULT_ZPID_SPARQL_ENDPOINT_URL = "http://kobpsy.zpid.de:8890/sparql";

	protected static Config INSTANCE;
	private static Logger LOGGER = null;
	private static final String CONFIG_PROPERTIES_FILE_NAME = "config.properties";


	private Properties properties;
    private String institutionUrl;
    private Path propFilePath;

	protected Config() {
		super();
	    LOGGER = LogManager.getLogger(Config.class);
		initialize();
	}

	protected static Config getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Config();
		}
		return INSTANCE;
	}

	protected void initialize() {
		try {
			propFilePath = getPropfilePath();
			properties = new Properties();
			properties.load(new FileInputStream(
					propFilePath.toFile()));
			StringWriter sw = new StringWriter();
			properties.store(sw, null);
			LOGGER.info("Annotating with the following configuration properties: ");
			LOGGER.info(sw.toString());
			institutionUrl = getBaseURI() + "/";
		} catch (IOException | URISyntaxException e) {
			LOGGER.error("Unable to locate the properties file");
			e.printStackTrace();
		}
	}

	/**
	 * Loads the properties file.
	 *
	 * @return the properties file
	 * @throws IOException
	 * @throws URISyntaxException
	 */
    protected Path getPropfilePath() throws IOException, URISyntaxException {
		URI jarpath = Config.class.getProtectionDomain().getCodeSource().getLocation().toURI();
		LOGGER.debug("jarpath: " + jarpath.toString());
		Path parentPath = Paths.get(jarpath).getParent();
		propFilePath = Paths.get(parentPath.toString(), CONFIG_PROPERTIES_FILE_NAME);
		if (!Files.exists(propFilePath, LinkOption.NOFOLLOW_LINKS)) {
			URL projectLocalPropertyFile = Config.class.getClassLoader().getResource(CONFIG_PROPERTIES_FILE_NAME);
			if (projectLocalPropertyFile != null) {
					propFilePath = Paths.get(projectLocalPropertyFile.toURI());
			}
		}
		LOGGER.debug("properties file was located here:: " + propFilePath);
		if (!Files.exists(propFilePath, LinkOption.NOFOLLOW_LINKS)) {
			throw new IOException();
		}
		return propFilePath;
	}

	/**
     * Base URI of the organization or institute that does the rdfization (e.g. "zpid").
     */
    public static String getInstitutionUrl() {
		return getInstance().institutionUrl;
	}

	protected String getProperty(String prop) {
        if (properties.containsKey(prop)) {
            return properties.getProperty(prop);
        }
        return (StringUtils.EMPTY);
    }


    /**
     * Gets the basic part of the URI for the generated resources.
     *
     * @return the base URI
     */
    private String getBaseURI() {
        String baseUri = getProperty("baseUri");
        if (baseUri.isEmpty()) {
        	LOGGER.error("No base URI provided in Properties file!");
        }
        return baseUri;
    }

    /**
     * Gets the basic part of the URI for the generated resources.
     *
     * @return the base URI
     */
    public static String getProxy() {
        return getInstance().getProperty("conn.proxy");
    }


    //NCBO Annotator
    public static String getNCBOServiceURL(){
    	return (getInstance().getProperty("ncbo.service.url"));
    }

    public static String getNCBOAPIKey(){
    	return (getInstance().getProperty("ncbo.apikey"));
    }

    public static String getNcboStopwords(){
    	return getInstance().getProperty("ncbo.annotator.stopwords");
    }

    public static boolean getNcboIsExcludeSynonyms(){
    	if (getInstance().getProperty("ncbo.annotator.excludeSynonyms")
    			.compareToIgnoreCase(Boolean.TRUE.toString()) == 0) {
    		return true;
    	}
    	return false;
    }

    public static boolean getNcboExpandMappings() {
    	if (getInstance().getProperty("ncbo.annotator.expandMappings")
    			.compareToIgnoreCase(Boolean.TRUE.toString()) == 0) {
    		return true;
    	}
    	return false;
    }

    public static boolean getNcboCui() {
    	if (getInstance().getProperty("ncbo.annotator.cui")
    			.compareToIgnoreCase(Boolean.TRUE.toString()) == 0) {
    		return true;
    	}
    	return false;
    }

    public static boolean getNcboSemanticType() {
    	if (getInstance().getProperty("ncbo.annotator.semanticType")
    			.compareToIgnoreCase(Boolean.TRUE.toString()) == 0) {
    		return true;
    	}
    	return false;
    }

    public static String getNCBOBaseConceptUri() {
    	return (getInstance().getProperty("ncbo.baseConceptUri"));
    }

    //UMLS Annotator
    public static String getUMLSUsername(){
    	return (getInstance().getProperty("umls.username"));
    }

    public static String getUMLSPassword(){
    	return (getInstance().getProperty("umls.password"));
    }

    public static String getUmlsOntologiesAsString() {
		return getInstance().getProperty("umls.annotator.ontologies");
    }
    public static String getNcboOntologiesAsString() {
    	return getInstance().getProperty("ncbo.annotator.ontologies");
    }

    public static Set<String> getUmlsOntologiesAsSet() {
    		StringTokenizer tokenizer = new StringTokenizer(getInstance().getProperty("umls.annotator.ontologies"), ",");
    		Set<String> ontologySet = new HashSet<>();
    		while (tokenizer.hasMoreTokens()) {
    			ontologySet.add(tokenizer.nextToken());
    		}
    		return ontologySet;
    }

    public static String getUmlsBaseConceptUri() {
    	return getInstance().getProperty("umls.baseConceptUri");
    }

    public static boolean isAddNcboConceptUris() {
    	if (getInstance().getProperty("umls.addNcboConceptUris").compareToIgnoreCase(Boolean.TRUE.toString()) == 0) {
    		return true;
    	}
    	return false;
    }

    /**
     * Returns the Version of the UMLS used by the
     * UTS services.
     *
     * @return the UMLS version for UTS services
     */
    public static String getUmlsVersionForUtsServices() {
    	return (getInstance().getProperty("umls.version.uts"));
    }

	public static String getUmlsMetamapOptions() {
		String options = getInstance().getProperty("umls.metamap.options");
		if (!StringUtils.isEmpty(options)) {
			return options;
		}
		return "";
	}

    //Other URLS
    public static String getPubMedURL(){
    	return (getInstance().getProperty("pubmed.url"));
    }

    public static String getDOIURL(){
    	return (getInstance().getProperty("doi.url"));
    }


    /**
     * The expected file types for articles to be processed.
     *
     * @return the suffix of the file type
     */
	public static String getInputFileSuffix() {
		return getInstance().getProperty("input.file.suffix");
	}

	public static String getLanguages() {
		String languages = getInstance().getProperty("languagesIncluded");
		if (StringUtils.isEmpty(languages)) {
			languages = "en";
		}
		return languages;
	}

	public static InputStream getOntologyMappingFile() {
		return Config.class.getClassLoader().
		getResourceAsStream("ontologyNameMappings_Ncbo_Umls.txt");
	}

	public static boolean isUseBrowserUrlAsConceptId() {
		if (getInstance().getProperty("annotation.browserUrlAsAnnotationTopic")
				.equalsIgnoreCase(Boolean.TRUE.toString())) {
			return true;
		}
		return false;
	}

	public static boolean isGenerateCrossrefLinks() {
		if (getInstance().getProperty("crossrefApi.links")
				.equalsIgnoreCase(Boolean.TRUE.toString())) {
			return true;
		}
		return false;
	}

	public static String getZpidSparqlEndpointUrl() {
		String zpidSparqlUrl = getInstance().getProperty("zpid.SparqlEndpointUrl");
		//TODO create this property in properties file
		if (!zpidSparqlUrl
			.isEmpty()) {
				return zpidSparqlUrl;
			}
		return DEFAULT_ZPID_SPARQL_ENDPOINT_URL;
	}

	public static boolean isSemanticType() {
		if (getInstance().getProperty("ncbo.annotator.semanticType")
			.equalsIgnoreCase(Boolean.TRUE.toString())) {
			return true;
		}
		return false;
	}

	public static boolean isSaveAnnotationAsJson() {
		if (getInstance().getProperty("ncbo.annotator.json.serialize")
				.equalsIgnoreCase(Boolean.TRUE.toString())) {
				return true;
		}
		return false;
	}
	public static boolean isGeneratePubPsychLinks() {
		if (getInstance().getProperty("pubpsych.links")
				.equalsIgnoreCase(Boolean.TRUE.toString())) {
			return true;
		}
		return false;
	}

	protected Properties getProperties() {
		return properties;
	}

	protected void setPropFilePath(Path path) {
		this.propFilePath = path;
	}

}