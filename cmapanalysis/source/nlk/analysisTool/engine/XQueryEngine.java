package nlk.analysisTool.engine;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;

import net.sf.saxon.Configuration;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;
import nlk.analysisTool.gui.AnalysisPanel;
import nlk.analysisTool.model.Analysis;

public class XQueryEngine
{
   /**
    * Run the provide xquery and serialize its output
    * as XML to another file. 
    */
	static Logger logger = Logger.getLogger(XQueryEngine.class.getName());

   public static void runXquery(String xquery, String resultFilePath) throws XPathException, IOException {
	   logger.info("Starting XQuery at " + new Date());
      long start = System.currentTimeMillis();
       final Configuration config = new Configuration();
       final StaticQueryContext sqc = new StaticQueryContext(config);
       final XQueryExpression exp = sqc.compileQuery(new StringReader(xquery));
       final DynamicQueryContext dynamicContext = new DynamicQueryContext(config);
       final Properties props = new Properties();
       props.setProperty(OutputKeys.METHOD, "xml");
       StreamResult sr  = new StreamResult(new File(resultFilePath));
       exp.run(dynamicContext, sr, props);
       sr.getOutputStream().close();
       long duration = System.currentTimeMillis() - start;
       logger.info("Finished XQuery, it took ms " + duration);
   }
	
	
}