package nlk.analysisTool.cxl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nlk.analysisTool.gui.AnalysisPanel;
import nlk.annotate.Annotations;
import nlk.base.ConceptMap;
import nlk.dtclient.DTLinksPart;
import nlk.presentation.PresentationList;
import nlk.resio.WriteInfo;
import nlk.soup.client.SoupClient;
import nlk.util.MIMETypeFileInfoMapper;
import nlk.xml.CXLExporter;
import nlk.xml.CXLImporter;
import nlk.xml.annotate.AnnotationsXMLHelper;
import nlk.xml.dt.DTLinksPartXMLHelper;
import nlk.xml.presentation.PresentationXMLHelper;
import nlk.xml.soup.SoupClientXMLHelper;
import nlk.xml.util.XMLHelper;

import org.apache.axiom.om.OMElement;
import org.xml.sax.SAXException;

public class CXLExtractor
{
	static Logger logger = Logger.getLogger(CXLExtractor.class.getName());

	public static void convertCmapToCXL (File cmapFile, File cxlFile) throws IOException, ClassNotFoundException, SAXException
    {
        String funcname = "CXLExtractor::convertCmapToCXL: ";
        InputStream inputStream = null;

        inputStream = new ZipInputStream (new FileInputStream (cmapFile));

        ZipEntry ze = null;

        while ((ze = (ZipEntry) ((ZipInputStream)inputStream).getNextEntry()) != null) {
            if (ze.getName().equalsIgnoreCase ("cmap")) {
                break;
            }
            if (ze.getName().equalsIgnoreCase ("cxl")) {
                break;
            }
        }

        if (ze == null) {
        	logger.warning (funcname + "Could not find entry for cmap or cxl in file " + cmapFile);

            try {inputStream.close();} catch (Exception e) {}
            inputStream = null;
            return;
        }

        if (ze.getName().equals("cmap"))
        {      	
	        // Must convert from java-serialized format to XML.
	        ObjectInputStream ois = new ObjectInputStream (inputStream);
	        ConceptMap cmap = (ConceptMap) ois.readObject();
	        CXLExporter.writeEncodedMap (cmap, cxlFile.getPath());
        }
        else
        {
        	// Just extract the cxl part
        	
        }
    }

}