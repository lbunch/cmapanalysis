package nlk.analysisTool.model;

import org.jdom.Attribute;
import org.jdom.Element;

public class Reference
{
	private String _id = null;
	private String _filePath = null;
	
	public Reference (String id, String filePath)
	{
		_id = id;
		_filePath = filePath;
	}
	
	public Reference (Reference r)
	{
		_id = r._id;
		_filePath = r._filePath;
	}
	
	public Reference (Element referenceXMLElement)
	{
		Attribute idAttrib  = referenceXMLElement.getAttribute("id");
		if (idAttrib != null)
		{
			_id = idAttrib.getValue();
		}
		_filePath = referenceXMLElement.getValue();
	}
	
	public String get_id()
	{
		return _id;
	}

	public String get_filePath() {
		return _filePath;
	}

	public void set_filePath(String path) {
		_filePath = path;
	}
}