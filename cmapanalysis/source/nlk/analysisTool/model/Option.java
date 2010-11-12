package nlk.analysisTool.model;

import org.jdom.Attribute;
import org.jdom.Element;

public class Option
{
	private String _id = null;
	private String _value = null;
	
	public Option (String id, String value)
	{
		_id = id;
		_value = value;
	}
	
	public Option (Option p)
	{
		_id = p._id;
		_value = p._value;
	}
	
	public Option (Element optionXMLElement)
	{
		Attribute idAttrib  = optionXMLElement.getAttribute("id");
		if (idAttrib != null)
		{
			_id = idAttrib.getValue();
		}
		_value = optionXMLElement.getValue();
	}
	
	public String get_id()
	{
		return _id;
	}

	public String get_value() {
		return _value;
	}

	public void set_value(String value) {
		_value = value;
	}
}