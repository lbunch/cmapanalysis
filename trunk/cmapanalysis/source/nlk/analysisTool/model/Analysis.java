package nlk.analysisTool.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.Configuration;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;
import nlk.analysisTool.cxl.CXLExtractor;
import nlk.analysisTool.engine.XQueryEngine;
import nlk.analysisTool.gui.AnalysisPanel;

import org.jdom.JDOMException;
import org.xml.sax.SAXException;

public class Analysis
{
	static Logger logger = Logger.getLogger(Analysis.class.getName());

	public static final String XQUERY_ANALYSIS_TEMPLATE_PATH = "./xquery/analysis-shell.xml";
	public static final String XQUERY_COLUMN_HEADER_TEMPLATE_PATH = "./xquery/column-header.xml";
	public static final String XQUERY_CELL_VALUE_TEMPLATE_PATH = "./xquery/cell-value.xml";

	
	public static final String INPUT_DIR_MARKER = "[[**input-directory**]]";
	 
	public static final String FUNCTION_MARKER = "[[**XQueryFunction**]]";
	
	public static final String COL_HEADERS_MARKER = "[[**column-header**]]";
	
	public static final String CELL_VALUES_MARKER = "[[**cell-value**]]";
	public static final String LABEL_MARKER = "[[**Label**]]";
	public static final String DATA_TYPE_MARKER = "[[**DataType**]]";
	public static final String XQUERY_MARKER = "[[**XQueryExpression**]]";

	private String _inputDirectoryPath = null;
	private List<Measurement> _measurementList = null;
	private String _outputFilePath = null;
	
	public Analysis(String inputDirPath, List<Measurement> measurements, String outputFilePath) throws FileNotFoundException
	{
		_inputDirectoryPath = verifyInputDirectory(inputDirPath);
		_measurementList = measurements;
		_outputFilePath = verifyOutputDirectory(outputFilePath);
	}
	
	/*
	 * <cmap-analysis>
	 *  <input-directory-path>C:\desktop\mapsFolder</input-directory-path>
	 *  <output-file-path>C:\desktop\result.xls</output-file-path>
	 	<cmap-measure>
			<Label>Map Name</Label>
			<Description>The filename of the concept map</Description>
			<DataType>String</DataType>
			<XQueryExpression>
			
			$map/cm:cmap/cm:res-meta/dc:title/text()
			
			</XQueryExpression>
			<XQueryFunctions></XQueryFunctions>
		</cmap-measure>
	 */		
	private Analysis(String analysisFilePath)
	{	
		// load a saved Analysis fil
	}
	
	public void saveToFile(String analysisFilePath)
	{
		// save analysis as xml
		
	}
	
	public void runAnalysis() throws FileNotFoundException, IOException, XPathException
	{
		File cmapDir = new File(_inputDirectoryPath);
		
		File cxlTempDir  = new File(cmapDir.getParent() + File.separatorChar + "tempAnalysisCxl"); 
		if (!cxlTempDir.exists())
		{
			cxlTempDir.mkdir();
		}
		try
		{
			convertCmapsToCXL(cmapDir, cxlTempDir, true);
			
			String queryTemplate = getFileContents(XQUERY_ANALYSIS_TEMPLATE_PATH);		
			String columnHeaderTemplate = getFileContents(XQUERY_COLUMN_HEADER_TEMPLATE_PATH);
			String cellValueTemplate = getFileContents(XQUERY_CELL_VALUE_TEMPLATE_PATH);
			
			String functionStrings = "";
			String colHeaderStrings = "";
			String xqueryStrings = "";
			
			// add the functions, column header, and xquery expression for each measurement to the query template
			for (Measurement m: _measurementList)
			{
				String cft = new String(m.get_xqueryFunctions());
				
				String cht = new String(columnHeaderTemplate);
				cht = cht.replace(LABEL_MARKER, m.get_label());
				colHeaderStrings += "\n" + cht;
				
				String cvt = new String (cellValueTemplate);
				cvt = cvt.replace(LABEL_MARKER, m.get_label());
				cvt = cvt.replace(DATA_TYPE_MARKER, m.get_dataType());
				cvt = cvt.replace(XQUERY_MARKER, m.get_xqueryExpression());
				for (Reference r: m.get_references())
				{
					String refPlaceholder = "[[**Reference_" + r.get_id() + "**]]";
					if (r.get_filePath().length() == 0)
					{
						throw new FileNotFoundException("Need to select a reference file for measurement: " + m.get_label());
					}
					File refFile = new File(r.get_filePath());
					if (!refFile.exists())
					{
						throw new FileNotFoundException("Invalid reference file " + r.get_filePath() + " for measurement: " + m.get_label());
					}
					String refURI = refFile.toURI().toString();
					cvt = cvt.replace(refPlaceholder, refURI);
					cft = cft.replace(refPlaceholder, refURI);
				}
				for (Option p: m.get_options())
				{
					String optPlaceholder = "[[**Option_" + p.get_id() + "**]]";
					String optValue = p.get_value();
					if (optValue.length() == 0)
					{
						throw new IOException("Need to enter an option value for measurement: " + m.get_label());					
					}
					cvt = cvt.replace(optPlaceholder, optValue);
					cft = cft.replace(optPlaceholder, optValue);
				}
				xqueryStrings += "\n" + cvt;
				functionStrings += "\n" + cft;
			}
			queryTemplate = queryTemplate.replace(FUNCTION_MARKER, functionStrings);
			queryTemplate = queryTemplate.replace(COL_HEADERS_MARKER, colHeaderStrings);
			
			File inFile = cxlTempDir;
			String collectionDir = inFile.toURI().toString();
			
			queryTemplate = queryTemplate.replace(INPUT_DIR_MARKER, collectionDir);
			queryTemplate = queryTemplate.replace(CELL_VALUES_MARKER, xqueryStrings);
			
			logger.info(queryTemplate);
			XQueryEngine.runXquery(queryTemplate, _outputFilePath.replace('\\', '/'));
		}
		finally
		{
			deleteDirectory(cxlTempDir);
		}
	}

	private void deleteDirectory(File dir)
	{
		File[] files = dir.listFiles();
		for (File f : files)
		{
			f.delete();
		}
		dir.delete();
	}
	
	private void convertCmapsToCXL(File cmapDirectory, File cxlDirectory, boolean clearDestination) throws FileNotFoundException
	{
		if (!cmapDirectory.exists() || !cmapDirectory.isDirectory())
		{
			throw new FileNotFoundException("Directory not found: " + cmapDirectory);
		}
		if (!cxlDirectory.exists() || !cxlDirectory.isDirectory())
		{
			throw new FileNotFoundException("Directory not found: " + cxlDirectory);
		}
		if (clearDestination)
		{
			// clear the cxl directory
			cxlDirectory.delete();
			cxlDirectory.mkdir();
		}
		File[] cmapFiles = cmapDirectory.listFiles();
		for (File cmapFile : cmapFiles)
		{
			String childFileName = cmapFile.getName();
			if (childFileName.endsWith(".cmap"))
			{
				File cxlFile = new File(cxlDirectory.getPath() + File.separatorChar + cmapFile.getName().substring(0,cmapFile.getName().lastIndexOf(".cmap")) + ".cxl");
				try {
					CXLExtractor.convertCmapToCXL(cmapFile, cxlFile);
				} catch (IOException e) {
					logger.log(Level.WARNING, "", e);
				} catch (ClassNotFoundException e) {
					logger.log(Level.WARNING, "", e);
				} catch (SAXException e) {
					logger.log(Level.WARNING, "", e);
				}
			}
			else if (childFileName.endsWith(".cxl"))
			{
				// Just copy the cxl file over
				File cxlFile = new File(cxlDirectory.getPath() + File.separatorChar + cmapFile.getName());
				try {
					copyFile(cmapFile, cxlFile);
				} catch (IOException e) {
					logger.log(Level.WARNING, "", e);
				}
			}
			else if (cmapFile.isDirectory() && !childFileName.startsWith("."))
			{
				convertCmapsToCXL(cmapFile, cxlDirectory, false);
			}
		}
	}
    
	private static void copyFile(File inputFile, File outputFile) throws IOException
	{
	    FileReader in = new FileReader(inputFile);
	    FileWriter out = new FileWriter(outputFile);
	    int c;

	    while ((c = in.read()) != -1)
	      out.write(c);

	    in.close();
	    out.close();
	}    
	
	public String verifyInputDirectory(String inputDirectoryPath) throws FileNotFoundException
	{
		if (inputDirectoryPath == null)
		{
			throw new FileNotFoundException("Input directory is null.");
		}
		File f = new File(inputDirectoryPath);
		if (!f.exists())
		{
			throw new FileNotFoundException("Input directory does not exist. " + inputDirectoryPath);
		}
		if (!f.isDirectory())
		{
			throw new FileNotFoundException("Input path is not a directory. " + inputDirectoryPath);
		}
		return inputDirectoryPath;
	}
	
	public String verifyOutputDirectory(String outputDirectoryPath) throws FileNotFoundException
	{
		if (outputDirectoryPath == null)
		{
			throw new FileNotFoundException("Output directory is null.");
		}
		File f = new File(outputDirectoryPath);
		if (f.isDirectory())
		{
			throw new FileNotFoundException("Output file path is a directory. " + outputDirectoryPath);
		}
		return outputDirectoryPath;
	}

	
	public String getFileContents(String filePath) throws FileNotFoundException, IOException
	{
		StringBuffer buffer = new StringBuffer();
		FileInputStream fis = new FileInputStream(filePath);
		int c = 0;
		while((c = fis.read()) != -1)
		{
			buffer.append((char)c);
		}
		return  buffer.toString();

	}
	

}