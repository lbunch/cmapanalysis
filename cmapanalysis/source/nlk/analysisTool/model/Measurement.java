package nlk.analysisTool.model;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.EscapeStrategy;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;


public class Measurement implements Comparable
{
	static Logger logger = Logger.getLogger(Measurement.class.getName());

	private File _sourceFile = null;
	private String _label = null;
	private String _description = null;
	private String _dataType = null;
	private List<Reference> _references = null;
	private List<Option> _options = null;
	private String _xqueryExpression = null;
	private String _xqueryFunctions = null;
	
//	private static DataFlavor[] _dataFlavors = null;
//	private static DataFlavor _dataFlavor = null;
//	
//	public static final String MEASUREMENT_DATA_TYPE = "class=nlk.analysisTool.model.Measurement";
//	
//	public static DataFlavor getDataFlavor()
//	{
//		if (_dataFlavor != null)
//		{
//			return _dataFlavor;
//		}
//		try 
//		{
//			_dataFlavor = new DataFlavor(Measurement.class, MEASUREMENT_DATA_TYPE);
//		} 
//		catch (NullPointerException e) 
//		{
//			// class is null
//			e.printStackTrace();
//		}
//		return _dataFlavor;
//	}
//	
//	public static DataFlavor[] getDataFlavors()
//	{
//		if (_dataFlavors != null)
//		{
//			return _dataFlavors;
//		}
//		_dataFlavors = new DataFlavor[]{getDataFlavor()};
//		return _dataFlavors;
//	}
//	
	public Measurement(File measureFile) throws IOException, JDOMException
	{
		_sourceFile = measureFile;
		String label = measureFile.getName();
		int dotIdx = label.lastIndexOf('.');
		if (dotIdx > 0)
		{
			label = label.substring(0, dotIdx);
		}
		_label = label;
		
		// Label, Description, DataType, 0* Reference id="", XQueryExpression, XQueryFunctions
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(measureFile);
		String s = parseElementValue(doc, "Label");
		if (s != null)
		{
			_label = s;
		}
		s = parseElementValue(doc, "Description");
		_description = s;
		s = parseElementValue(doc, "DataType");
		_dataType = s;
		s = parseElementContent(doc, "XQueryExpression");
		_xqueryExpression = s;
		s = parseElementContent(doc, "XQueryFunctions");
		_xqueryFunctions = s;
		_references = parseReferenceList(doc);
		_options = parseOptionList(doc);
	}
	
	public Measurement(Measurement m)
	{
		_sourceFile = m._sourceFile;
		_label = m._label;
		_description = m._description;
		_dataType = m._dataType;
		_xqueryExpression = m._xqueryExpression;
		_xqueryFunctions = m._xqueryFunctions;
		_references = new ArrayList<Reference>();
		for (Reference r: m.get_references())
		{
			Reference rclone = new Reference(r);
			_references.add(rclone);
		}
		_options = new ArrayList<Option>();
		for (Option p : m.get_options())
		{
			Option pclone = new Option(p);
			_options.add(pclone);
		}
	}
	
	private String parseElementValue(Document doc, String elementName) throws IOException
	{
		ElementFilter filter = new ElementFilter(elementName);
		Iterator<Element> iter = doc.getDescendants(filter);
		if (iter.hasNext())
		{
			Element e = iter.next();
			return e.getValue();
		}
		return null;
	}

	private String parseElementContent(Document doc, String elementName) throws IOException
	{
		ElementFilter filter = new ElementFilter(elementName);
		Iterator<Element> iter = doc.getDescendants(filter);
		if (iter.hasNext())
		{
			Element e = iter.next();
			XMLOutputter xout = new XMLOutputter();
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			xout.output(e.getContent(), outStream);
			String s = outStream.toString();
			s = s.replace("&lt;", "<");
			s = s.replace("&gt;", ">");
			return s;
		}
		return null;
	}
	
	private List<Reference> parseReferenceList(Document doc)
	{
		List<Reference> refList = new ArrayList<Reference>();
		ElementFilter filter = new ElementFilter("Reference");
		Iterator<Element> elemIter = doc.getDescendants(filter);
		while (elemIter.hasNext())
		{
			refList.add(new Reference(elemIter.next()));
		}
		return refList;
	}
	
	private List<Option> parseOptionList(Document doc)
	{
		List<Option> optList = new ArrayList<Option>();
		ElementFilter filter = new ElementFilter("Option");
		Iterator<Element> elemIter = doc.getDescendants(filter);
		while (elemIter.hasNext())
		{
			optList.add(new Option(elemIter.next()));
		}
		return optList;
	}

	
	public String toString()
	{
		return _label;
	}
	
	
	public static DefaultTreeModel createMeasuresTreeModel(String measurementDirPath) throws FileNotFoundException
	{
		if (measurementDirPath == null)
		{
			throw new FileNotFoundException("Measurement directory is null.");
		}
		File f = new File(measurementDirPath);
		if (!f.exists())
		{
			throw new FileNotFoundException("Measurement directory does not exist. " + measurementDirPath);
		}
		if (!f.isDirectory())
		{
			throw new FileNotFoundException("Measurement path is not a directory. " + measurementDirPath);
		}

		DefaultMutableTreeNode rootNode = getTreeNodeForFile(f);
		DefaultTreeModel measuresModel = new DefaultTreeModel(rootNode);
		return measuresModel;
	}
	
	private static DefaultMutableTreeNode getTreeNodeForFile(File measurementFile)
	{
		DefaultMutableTreeNode treeNode = null;
		
		if (measurementFile.isDirectory())
		{
			// add a node for the directory
			treeNode = new DefaultMutableTreeNode(measurementFile.getName());
			// recursive call to add each child
			File[] files = measurementFile.listFiles();
			List<File> flist = new ArrayList<File>();
			
			for (File childFile: files)
			{
				flist.add(childFile);
			}
			Collections.sort(flist);
			
			for (File childFile: flist)
			{
				if (childFile.getName().startsWith(".") || childFile.getName().equals("CVS"))
				{
					continue;
				}
				else
				{
					treeNode.add(getTreeNodeForFile(childFile));
				}
			}
		}
		else
		{
			try
			{
				// add a leaf node containing a Measurement read from the file
				Measurement m = new Measurement(measurementFile);
				treeNode = new DefaultMutableTreeNode(m);	
			}
			catch(Exception e)
			{
				logger.log(Level.WARNING, "", e);
			}
		}
		return treeNode;
	}

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		return this;
	}

//	public DataFlavor[] getTransferDataFlavors() {
//		return getDataFlavors();
//	}
//
//	public boolean isDataFlavorSupported(DataFlavor flavor) {
//		return flavor.equals(Measurement.getDataFlavor()) || flavor.equals(MeasurementList.getDataFlavor());
//	}

	public String get_label() {
		return _label;
	}

	public void set_label(String _label) {
		this._label = _label;
	}

	public List<Reference> get_references() {
		return _references;
	}

	public void set_references(List<Reference> _references) {
		this._references = _references;
	}

	public List<Option> get_options()
	{
		return _options; 
	}

	public void set_options(List<Option> options) {
		this._options = options;
	}

	public String get_dataType() {
		return _dataType;
	}

	public String get_description() {
		return _description;
	}

	public String get_xqueryExpression() {
		return _xqueryExpression;
	}

	public String get_xqueryFunctions() {
		return _xqueryFunctions;
	}

	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}

}