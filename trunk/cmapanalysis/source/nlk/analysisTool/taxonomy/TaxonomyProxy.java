package nlk.analysisTool.taxonomy;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.net.URI;
import java.net.URL;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.FeatureKeys;
import net.sf.saxon.event.ContentHandlerProxy;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.Sender;
import net.sf.saxon.event.SerializerFactory;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tinytree.TinyDocumentImpl;
import net.sf.saxon.trans.XPathException;
import nlk.analysisTool.gui.AnalysisPanel;
import nlk.base.ConceptMap;
import nlk.util.taxonomy.TopologicalTaxonomy;
import nlk.xml.CXLImporter;

public class TaxonomyProxy
{
	static Logger logger = Logger.getLogger(TaxonomyProxy.class.getName());

	public static int calcTaxonomyScore(NodeInfo ni)
	{
		try
		{
			logger.info(ni.getSystemId());
			long startTime = System.currentTimeMillis();
			
			/*lkb UTF-8 char conversion problem
			String cmXML = new String(serialize(ni).getBytes("UTF-8"), "UTF-8");
			logger.info("serialize " + (System.currentTimeMillis() - startTime));
			
			startTime = System.currentTimeMillis();
			ConceptMap cm = CXLImporter.getDecodedMap(new StringBufferInputStream(cmXML));
			logger.info("decode " + (System.currentTimeMillis() - startTime));
			*/
			URL u = new URL(ni.getSystemId());
			String cmFilePath = u.getFile();
			cmFilePath = cmFilePath.replace("%20", " ");
			ConceptMap cm = CXLImporter.getDecodedMap(new FileInputStream(cmFilePath));
			// end UTF-8 char conversion workaround
			
			startTime = System.currentTimeMillis();
			Hashtable<String,String> mapFeatures = new Hashtable<String,String>();
			//mapFeatures.put(TopologicalTaxonomy.CMAP_CROSSLINK_COUNT_KEY, "");
			int taxLevel = TopologicalTaxonomy.getLevel(cm, mapFeatures);
			logger.info("time to compute score: " + (System.currentTimeMillis() - startTime));
			logger.info("features: " + mapFeatures);
			
			return taxLevel;
		}
		catch(Exception e)
		{
			logger.log(Level.WARNING,"", e);
		}
		return -1;
	}

	public static String serialize(NodeInfo nodeInfo) throws XPathException 
	{
		Configuration config = nodeInfo.getConfiguration();
		SerializerFactory sf;
		StringWriter sw = new StringWriter();
		Properties props = new Properties();
		props.setProperty("method", "xml");
		props.setProperty("indent", "yes");
		props.setProperty("encoding", "UTF-8");
		Receiver serializer = config.getSerializerFactory()
				.getReceiver(new StreamResult(sw),
						config.makePipelineConfiguration(), props);
		nodeInfo.copy(serializer, NodeInfo.ALL_NAMESPACES, true, 0);
		return sw.toString();
	} 
	
	public static String test(String val)
	{
		return "2";
	}

	public static void test()
	{

	}
	
}