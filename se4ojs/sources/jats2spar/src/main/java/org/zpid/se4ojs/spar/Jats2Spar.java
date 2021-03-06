package org.zpid.se4ojs.spar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.MessageListener;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * <p>
 * This class performs the transformation from a JATS-XML input-file to the
 * adapted version of the JATStoRDF stylesheet by Peroni et al.
 * </p>
 *
 * @author barth
 *
 */
public class Jats2Spar {
	private static final String JATS__1_0_XSD = "/jats-publishing-xsd-1.0/JATS-journalpublishing1.xsd";

	private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";

	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

	/** The adapted stylesheet. */
	private static final String RESOURCE_JATS2SPAR_XSL = "/jats2spar_zpid.xsl";

	/**
	 * A JATS-article that contains most of the processed JATS-tags. Useful for
	 * testing.
	 */

	private static final String TAG_ARTICLE = "article";

	private static final String ATTR_LANGUAGE = "xml:lang";

	/** The object representing the XML-document. */
	static Document document;

	private static final Logger log = LogManager.getLogger(Jats2Spar.class);

	private String articleLanguage;

	/**
	 * <p>
	 * Initializes the SAX-transformer, sets the base URI of the RDF triples
	 * according to the specification in the config.properties file and carries
	 * out the transformation.
	 * </p>
	 * <p>
	 * The article language is checked prior to transformation. If it is not part of the
	 * user-configured accepted languages, transformation is aborted and the method returns false.
	 * </p>
	 *
	 * @param dataFile
	 *            the file to transform
	 * @param outputFile
	 *            the name of the file that contains the transformed RDf
	 * @param baseUrl
	 * 			  the base url for the generated rdf resources
	 * @return false if the article language is not accepted
	 * @throws URISyntaxException
	 */
	public boolean transform(final File dataFile, File outputFile, String baseUrl, String languages)
			throws URISyntaxException {

		log.info(String.format("Starting xslt transformation for file: %s",
				dataFile));
		try {
			javax.xml.parsers.DocumentBuilder builder = initTransformation();
			//Workaround to avoid search for DTDs:
			//            instead of using dummy DTD / xml catalog, the paper is read into a document
			//            and then passed to the sax transformer.
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			Document doc = builder.parse(dataFile);
			xformer.transform(new DOMSource(doc), new StreamResult(out));

			byte[] data = out.toByteArray();
			ByteArrayInputStream istream = new ByteArrayInputStream(data);

			if (checkArticleLanguage(doc, languages)) {
				doTransform(istream, dataFile, outputFile, baseUrl);
				return true;
			} else {
				log.warn("Unsupported article language: " + articleLanguage + ". Processing of article " + dataFile + "aborted.");
			}
		} catch (ParserConfigurationException | SAXException | IOException
				| TransformerFactoryConfigurationError | TransformerException | SaxonApiException e) {
			log.error("\n** Transformation error");
			log.error("   " + e.getMessage());

			// Use the contained exception, if any
			if (e.getSuppressed() != null) {
				for (Throwable sup : e.getSuppressed()) {
					sup.printStackTrace();
					log.error("Suppressed exception: ", sup);
				}
			}
		}
		return false;
	}

	/**
	 * Prepares the transformation. Initializes the {@link DocumentBuilderFactory}.
	 * @return the {@link DocumentBuilder}
	 * @throws ParserConfigurationException
	 */
	private javax.xml.parsers.DocumentBuilder initTransformation()
			throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://xml.org/sax/features/namespaces", false);
		factory.setFeature("http://xml.org/sax/features/validation", false);
		factory.setFeature(
				"http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
				false);
		factory.setFeature(
				"http://apache.org/xml/features/nonvalidating/load-external-dtd",
				false);

		factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
		String schemaSource = getClass().getResource(JATS__1_0_XSD)
				.toString();
		factory.setAttribute(JAXP_SCHEMA_SOURCE, new File(schemaSource));
		javax.xml.parsers.DocumentBuilder builder = factory
				.newDocumentBuilder();
		return builder;
	}

	private void doTransform(InputStream istream, File dataFile, File outputFile, String baseUrl)
			throws SaxonApiException, TransformerFactoryConfigurationError, TransformerException {

		InputStream stylesheetUri = Jats2Spar.class
				.getResourceAsStream(RESOURCE_JATS2SPAR_XSL);
		TransformerFactory tFactory = TransformerFactory.newInstance();
		TransformerFactoryImpl tFactoryImpl = (TransformerFactoryImpl) tFactory;
		net.sf.saxon.Configuration saxonConfig = tFactoryImpl
				.getConfiguration();
		saxonConfig
				.registerExtensionFunction(new CrossrefLinksByDoiExtensionFunctionDef());
		saxonConfig
			.registerExtensionFunction(new CrossrefSubjectsByDoiExtensionFunctionDef());
		saxonConfig
			.registerExtensionFunction(new PubPsychLinkByTitleExtensionFunctionDef());
		saxonConfig
		    .registerExtensionFunction(new PubpsychSubjectsByTitleExtensionFunctionDef());
		Processor proc = new Processor(saxonConfig);
		XsltCompiler comp = proc.newXsltCompiler();
		XsltExecutable exec;
		exec = comp.compile(new StreamSource(stylesheetUri));
		XsltTransformer transformer = exec.load();

		DocumentBuilder saxBuilder = proc.newDocumentBuilder();
		saxBuilder.setLineNumbering(true);
		saxBuilder.setDTDValidation(false);


		XdmNode source = saxBuilder.build(new StreamSource(istream));

		Serializer serializer = new Serializer();
		serializer.setOutputProperty(Serializer.Property.INDENT, "yes");
		serializer.setOutputFile(outputFile);
		transformer.setParameter(new QName("baseUri"), new XdmAtomicValue(
				baseUrl));
		transformer.setInitialContextNode(source);
		transformer.setDestination(serializer);
		transformer.setMessageListener(new MessageListener() {

			@Override
			public void message(XdmNode arg0, boolean arg1,
					SourceLocator arg2) {
				log.warn(String.format(
						"Transformation Error: %s\n   Line Number: %s",
						arg0, arg2.getLineNumber()));

			}
		});
		transformer.transform();
		transformer.close();
		log.info(String.format("Finished xslt transformation for file: %s",
				dataFile));
	}

	/**
	 * Checks whether the article language of the document
	 * is accepted (is present in config.properties).
	 * Default is English.
	 *
	 * @param languages the languages that are allowed
	 * @return true if the language is allowed, false otherwise
	 */
	private boolean checkArticleLanguage(Document doc, String languages) {
		Element article = (Element) doc.getElementsByTagName(TAG_ARTICLE).item(0);
		Attr attrLanguage = article.getAttributeNode(ATTR_LANGUAGE);
		articleLanguage = attrLanguage.getValue();
		if (languages.toLowerCase().contains(articleLanguage.toLowerCase())) {
			return true;
		}
		return false;
	}
}