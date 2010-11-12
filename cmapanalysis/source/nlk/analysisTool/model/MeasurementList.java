package nlk.analysisTool.model;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MeasurementList extends ArrayList<Measurement> implements Transferable
{
	static Logger logger = Logger.getLogger(MeasurementList.class.getName());

	private static DataFlavor[] _dataFlavors = null;
   private static DataFlavor _dataFlavor = null;
   
   public static final String MEASUREMENT_LIST_DATA_TYPE = "class=nlk.analysisTool.model.MeasurementList";
   
   public static DataFlavor getDataFlavor()
   {
      if (_dataFlavor != null)
      {
         return _dataFlavor;
      }
      try 
      {
         _dataFlavor = new DataFlavor(Measurement.class, MEASUREMENT_LIST_DATA_TYPE);
      } 
      catch (NullPointerException e) 
      {
         // class is null
         logger.log(Level.WARNING, "", e);
      }
      return _dataFlavor;
   }
   
   public static DataFlavor[] getDataFlavors()
   {
      if (_dataFlavors != null)
      {
         return _dataFlavors;
      }
      _dataFlavors = new DataFlavor[]{getDataFlavor()};
      return _dataFlavors;
   }
   

   public Object getTransferData(DataFlavor flavor)
         throws UnsupportedFlavorException, IOException {
       return this;
   }

   public DataFlavor[] getTransferDataFlavors() {
      return getDataFlavors();
   }

   public boolean isDataFlavorSupported(DataFlavor flavor) {
      return flavor.equals(getDataFlavor());
   }
   
}