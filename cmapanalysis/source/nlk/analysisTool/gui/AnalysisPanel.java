/*
 * Created by JFormDesigner on Thu Aug 28 11:47:25 CDT 2008
 */

package nlk.analysisTool.gui;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;

import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.*;
import javax.swing.tree.*;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.instruct.UserFunction;
import net.sf.saxon.om.*;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.QueryResult;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.Value;
import net.sf.saxon.xpath.XPathEvaluator;
import nlk.analysisTool.model.Analysis;
import nlk.analysisTool.model.Measurement;
import nlk.analysisTool.model.MeasurementList;
import nlk.analysisTool.model.Option;
import nlk.analysisTool.model.Reference;
import nlk.resourceViewers.externalViewer.MacExternalViewer;
import nlk.resourceViewers.externalViewer.Win32ExternalViewer;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import core.util.CoreProgressDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
/**
 * @author Lawrence Bunch
 */

public class AnalysisPanel extends JPanel {
	
	static Logger logger = Logger.getLogger(AnalysisPanel.class.getName());

	public static final String UI_FONT = "Verdana";
	public static final int MEASUREMENTS_ROW = 0;
	public static final int LABELS_ROW = 1;
	public static final int REFERENCES_ROW = 2;
	public static final int OPTIONS_ROW = 3;
	public static final int ACTIONS_ROW = 4;

	public static final int INFO_COLUMN = 0;
	
	public static final String measurementsDirectoryPath = "./measurements";
	public static final String deleteIconPath = "./config/delete-icon.gif";
	private File _lastInputDirectory = new File(System.getProperty("user.home"));
	private File _lastOutputFile = new File(System.getProperty("user.home"));
	private File _lastReferenceDirectory = new File(System.getProperty("user.home"));
	
	private JFrame _parentFrame = null;
	
	public AnalysisPanel(JFrame parentFrame) {
		_parentFrame = parentFrame;
		try 
		{
            UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception xcp) 
        {
        	logger.log(Level.WARNING, "", xcp);
        }
        	initComponents();
	}

	public static void main(String[] args) {
		JFrame f = new JFrame("Cmap Analysis Tool");
		f.addWindowListener(
	            new java.awt.event.WindowAdapter() {
	                public void windowClosing(java.awt.event.WindowEvent we) {
	                    System.exit(0);
	                }
	            });
		AnalysisPanel ap = new AnalysisPanel(f);
		f.getContentPane().add(ap);
		f.pack();
		f.setVisible(true);
	}
	
	private void folderBrowseButtonActionPerformed(ActionEvent e) {
		// show file chooser to select a folder of cmap xml files to process
		JFileChooser jfc = new JFileChooser(_lastInputDirectory);
		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int choice = jfc.showOpenDialog(this);
		if(choice == JFileChooser.APPROVE_OPTION)
		{
			_lastInputDirectory = jfc.getSelectedFile();
			folderPathTextField.setText(_lastInputDirectory.getPath());
		}


	}

	private void outputBrowseButtonActionPerformed(ActionEvent e) {
		// show file chooser to select a location and file name for the analysis results
		JFileChooser jfc = new JFileChooser(_lastOutputFile);
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int choice = jfc.showSaveDialog(this);
		if(choice == JFileChooser.APPROVE_OPTION)
		{
			_lastOutputFile = jfc.getSelectedFile();
			outputLocation.setText(_lastOutputFile.getPath());
		}
	}

	private void runAnalysisButtonActionPerformed(ActionEvent e) {
		// check that the input folder and output file have been selected
		final String inputDirPath = folderPathTextField.getText();
		File inDir = new File(inputDirPath);
		if (!inDir.exists())
		{
			ErrorDialog alertDialog = new ErrorDialog(_parentFrame, "Warning", "The input path does not exist: " + inputDirPath);
			alertDialog.showDialog();
			return;
		}
		if (!inDir.isDirectory())
		{
			ErrorDialog alertDialog = new ErrorDialog(_parentFrame, "Warning", "The input path is not a directory: " + inputDirPath);
			alertDialog.showDialog();
			return;
		}
		final String outputFilePath = outputLocation.getText();
		File outFile = new File(outputFilePath);
		if (!outFile.getParentFile().exists())
		{
			ErrorDialog alertDialog = new ErrorDialog(_parentFrame, "Warning", "The output location does not exist: " + outputFilePath);
			alertDialog.showDialog();
			return;
		}
		if (!outFile.getParentFile().exists() || outFile.isDirectory())
		{
			ErrorDialog alertDialog = new ErrorDialog(_parentFrame, "Warning", "The output location should be a file, not a directory: " + outputFilePath);
			alertDialog.showDialog();
			return;
		}
		final ArrayList<Measurement> measureList = new ArrayList<Measurement>(); 
		// add the measures from the table (one per row)
		TableModel tableModel = analysisTable.getModel();
		int colCount = tableModel.getColumnCount();
		for (int c = 1; c < colCount; c++)
		{
			int modelColIdx = analysisTable.getColumnModel().getColumn(c).getModelIndex();
			Measurement m = (Measurement)tableModel.getValueAt(MEASUREMENTS_ROW, modelColIdx);
			measureList.add(m);
		}
		// execute the xquery
		Thread t = new Thread("analysis"){
			public void run()
			{
				JDialog progressDlg = new JDialog(_parentFrame, "Running Cmap Analaysis", false);
				progressDlg.setPreferredSize(new Dimension(300, 100));
				JProgressBar progressBar = new JProgressBar(0, 100);
				progressBar.setIndeterminate(true);
				JLabel wait = new JLabel("Please wait");
				JPanel panel = new JPanel(new BorderLayout());
				panel.add(wait, BorderLayout.NORTH);
		        panel.add(progressBar, BorderLayout.CENTER);
		        progressDlg.getContentPane().add(panel, BorderLayout.PAGE_START);
		        progressDlg.pack();
				try
				{
					progressDlg.setVisible(true);
					Analysis a = new Analysis (inputDirPath, measureList, outputFilePath);
					a.runAnalysis();
					progressDlg.dispose();
					
					String osName = System.getProperty("os.name");
					String fileURLStr = "file://" + outputFilePath.replace('\\', '/');
					if (osName.contains("Windows"))
					{
						Win32ExternalViewer winViewer = new Win32ExternalViewer();
						winViewer.showURL(fileURLStr);
					}
					else if (osName.contains("Mac"))
					{
						MacExternalViewer macViewer = new MacExternalViewer();
						macViewer.showURL(fileURLStr);
					}
					else
					{
						ErrorDialog successDialog = new ErrorDialog(_parentFrame, "Success", "The analysis is complete and the results are available in " + outputFilePath);	
						successDialog.showDialog();
					}
				}
				catch(Throwable ex)
				{
					progressDlg.dispose();
					logger.log(Level.WARNING, "", ex);
					ErrorDialog alertDialog = new ErrorDialog(_parentFrame, "Error", "The analysis could not be completed: " + ex.getMessage());
					alertDialog.showDialog();		
				}
			}
		};
		t.start();
	}
	
	public String readFromFile(String filePath) throws FileNotFoundException, IOException
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
	
	public class MeasureTreeTransferHandler extends TransferHandler
	{
		public int getSourceActions(JComponent c) {
		    return COPY;
		}

		public Transferable createTransferable(JComponent c) {
			if (! (c instanceof JTree))
			{
				return null;
			}
			JTree tree = (JTree)c;
			TreePath[] selectionPaths = tree.getSelectionPaths();
			MeasurementList measureList = new MeasurementList();
			DefaultMutableTreeNode measureNode = null;
			
			for (int m = 0; m < selectionPaths.length; m++)
			{
			   measureNode = (DefaultMutableTreeNode)selectionPaths[m].getLastPathComponent();
			   if (measureNode.isLeaf())
	         {
	            Measurement measurement = (Measurement)measureNode.getUserObject();
	            measureList.add(measurement);
	         }
			   else
			   {
			      DefaultMutableTreeNode subMeasureNode = null;
			      for(int mc = 0; mc < measureNode.getChildCount(); mc++)
			      {
			         subMeasureNode = (DefaultMutableTreeNode)measureNode.getChildAt(mc);
		            if (subMeasureNode.isLeaf())
		            {
		               Measurement measurement = (Measurement)subMeasureNode.getUserObject();
		               measureList.add(measurement);
		            }
			      }
			   }
			}
			
			return measureList;
		}

		public void exportDone(JComponent c, Transferable t, int action) {
		    if (action == MOVE) {
		        // remove selected item, but ignore since we only support copy
		    }
		}
	}


   //16DnD     public boolean canImport(TransferHandler.TransferSupport transferSupport)
	// JDK 1.5 DnD
   public boolean canImport(Transferable transferSupport)
   {
      return false;
   }
   
   //16DnD     public boolean importData(TransferHandler.TransferSupport transferSupport)
  public boolean importData(Transferable transferSupport)
   {
      return false;
   }


	private class AnalysisTableTransferHandler extends TransferHandler
	{
		private JTable _dropTable = null;
		public AnalysisTableTransferHandler (JTable dropTable)
		{
			_dropTable = dropTable;
		}
		
		public int getSourceActions(JComponent c) {
		    return COPY_OR_MOVE;
		}

		public Transferable createTransferable(JComponent c) {
			return null; // no drag, only drop
		}

		public void exportDone(JComponent c, Transferable t, int action) {
		    if (action == MOVE) {
		        // remove selected item, but ignore since we only support copy
		    }
		}

//16DnD		public boolean canImport(TransferHandler.TransferSupport transferSupport)
      public boolean canImport(Transferable transferSupport)
		{
//			 Check for String flavor
		    if (!transferSupport.isDataFlavorSupported(MeasurementList.getDataFlavor())) {
		        return false;
		    }

		    // Fetch the drop location
		  //16DnD DropLocation loc = transferSupport.getDropLocation();

		    // Return whether we accept the location
		    
		    return true;
		    //return shouldAcceptDropLocation(loc);
		}
		
    //16DnD		public boolean importData(TransferHandler.TransferSupport transferSupport)
      public boolean importData(Transferable transferSupport)
		{
			if (!canImport(transferSupport)) {
		        return false;
		    }

		    // Fetch the Transferable and its data
//16DnD Transferable t = transferSupport.getTransferable();
			Transferable t = transferSupport;
		    try 
		    {
				Measurement measure = (Measurement)t.getTransferData(MeasurementList.getDataFlavor());
			    // Fetch the drop location
				//16DnD DropLocation loc = transferSupport.getDropLocation();
				//16DnD JTable.DropLocation tableLoc = (JTable.DropLocation)loc;
				
//			    if (tableLoc.isInsertColumn())
//			    {
//			    // Insert the data at this location
//			    	addMeasureColumn(measure, tableLoc.getColumn());
//					analysisTable.setRowHeight(MEASUREMENTS_ROW, 1);
//			    }
				addMeasureColumn(measure, 1);
			    return true;
			} 
		    catch (Exception e) 
			{
		    	logger.log(Level.WARNING, "", e);
			}
			return false;
		}		
		
		
		private int addMeasureColumn(Measurement measure, int columnIdx)
		{
			// don't allow adding to the left of the first column of labels
			if (columnIdx <= 0)
			{
				columnIdx = 1;
			}
			
			DefaultTableModel tm = (DefaultTableModel)_dropTable.getModel();
//			try
//			{
//			   _dropTable.getColumn(measure.get_label());
//			}
//			catch (IllegalArgumentException ex)
//			{
			   // column not found
   			tm.addColumn(measure.get_label(), new Object[] {measure, measure.get_label(), measure.get_references(), measure.get_options()});
   
   			TableColumn measureColumn = new TableColumn(tm.getColumnCount()-1, 100, new AnalysisCellRenderer(), new AnalysisCellEditor());
   			measureColumn.setIdentifier(measure.get_label());
   			measureColumn.setMinWidth(100);
   			measureColumn.setPreferredWidth(100);
   			_dropTable.addColumn(measureColumn);
   			_dropTable.getColumnModel().moveColumn(tm.getColumnCount() - 1, columnIdx);
   			columnIdx++;
//			}
			_dropTable.setRowHeight(MEASUREMENTS_ROW, 1);
		
   		return columnIdx;
   	}
	}
	
	private class AnalysisCellRenderer extends DefaultTableCellRenderer
	{
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
		{
            Component component =  super.getTableCellRendererComponent (table, value, isSelected, false, row, column);
            if (row == REFERENCES_ROW)
            {
            	JButton refButton = createReferenceButton(table, value, column);
            	if (refButton != null)
            	{
            		return refButton;
            	}
            }
            if (row == OPTIONS_ROW)
            {
            	
            }
            if (row == ACTIONS_ROW)
            {
            	JButton deleteButton = new JButton(new ImageIcon(deleteIconPath));
            	deleteButton.setContentAreaFilled(false);
            	deleteButton.setToolTipText("Delete Measurement");
            	return deleteButton;
            }
            // the default JTextArea is fine
			return component;
		}
		
		protected void firePropertyChange(String propertyName, Object oldValue, Object newValue)
		{

		}
	}
	
	
	private class AnalysisCellEditor extends DefaultCellEditor
	{
        public AnalysisCellEditor () 
        {
            super (new JTextField());
        }

        public Object getCellEditorValue() 
        {
            return super.getCellEditorValue();
        }
        
        public Component getTableCellEditorComponent (JTable table,
                                                      Object value,
                                                      boolean isSelected,
                                                      int row,
                                                      int column)
        {
        	if (row == REFERENCES_ROW)
            {
            	JButton refButton = createReferenceButton(table, value, column);
            	if (refButton != null)
            	{
            		return refButton;
            	}
            }
        	if (row == ACTIONS_ROW)
        	{
            	TableColumn tableColumn = table.getColumn(table.getColumnName(column));
            	JButton deleteButton = new JButton(new ImageIcon(deleteIconPath));
            	deleteButton.setContentAreaFilled(false);
            	deleteButton.setToolTipText("Delete Measurement");
            	deleteButton.addActionListener(new DeleteColumnActionListener(table, tableColumn));
            	return deleteButton;
        	}
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }

        public boolean isCellEditable(EventObject evt) {
            if (evt instanceof MouseEvent) {
                return ((MouseEvent)evt).getClickCount() >= 1;
            }
            return true;
        }

    }

	private class ReferenceActionListener implements ActionListener
	{
		private JTable parent = null;
		private Reference ref = null;
		
		public ReferenceActionListener(Reference r, JTable p)
		{
			ref = r;
			parent = p;
		}
		
		public void actionPerformed(ActionEvent e) 
		{
			JFileChooser jfc = new JFileChooser(_lastReferenceDirectory);
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int choice = jfc.showOpenDialog(parent);
			if(choice == JFileChooser.APPROVE_OPTION)
			{
				_lastReferenceDirectory = jfc.getSelectedFile().getParentFile();
				File newRefFile = jfc.getSelectedFile();
				ref.set_filePath(newRefFile.getPath());
			}
			parent.removeEditor();
		}
	}

	private class DeleteColumnActionListener implements ActionListener
	{
		private JTable table = null;
		private TableColumn column = null;
		
		public DeleteColumnActionListener(JTable t, TableColumn c)
		{
			table = t;
			column = c;
		}
		
		public void actionPerformed(ActionEvent e) 
		{
			removeColumnAndData(table, table.getColumnModel().getColumnIndex(column.getIdentifier()));
			table.setRowHeight(MEASUREMENTS_ROW, 1);
		}
	}

	private class AnalysisTableModel extends DefaultTableModel
	{
		public AnalysisTableModel(Object[][] data, String[] colNames)
		{
			super(data, colNames);
		}
		
		public boolean isCellEditable(int row, int column)
		{
			if (column == INFO_COLUMN || row == MEASUREMENTS_ROW)
			{
				return false;
			}
			Measurement m = (Measurement)this.getValueAt(MEASUREMENTS_ROW, column);
			if (row == REFERENCES_ROW)
			{
				List<Reference> refList = m.get_references();
				if (refList == null || refList.isEmpty())
				{
					return false;
				}
			}
			if (row == OPTIONS_ROW)
			{
				List<Option> optsList = m.get_options();
				if (optsList == null || optsList.isEmpty())
				{
					return false;
				}
			}
			return true;
		}
		
		public Object getValueAt(int row, int column)
		{
			if (column == INFO_COLUMN || row == MEASUREMENTS_ROW || row == LABELS_ROW || row == ACTIONS_ROW)
			{
				return super.getValueAt(row, column);
			}
			Measurement m = (Measurement)super.getValueAt(MEASUREMENTS_ROW, column);
			if (row == REFERENCES_ROW)
			{
				if(m.get_references() == null || m.get_references().size() == 0)
				{
					return "";
				}
				else
				{
					return m.get_references().get(0).get_filePath();
				}
			}
			if (row == OPTIONS_ROW)
			{
				if (m.get_options() == null || m.get_options().size() == 0)
				{
					return "";
				}
				else
				{
					return m.get_options().get(0).get_value();
				}
			}
			return super.getValueAt(row, column);
		}
		
		public void setValueAt(Object value, int row, int column)
		{
			super.setValueAt(value, row, column);
			if (row == LABELS_ROW)
			{
				Measurement m = (Measurement)super.getValueAt(MEASUREMENTS_ROW, column);
				m.set_label(value.toString());
			}
			else if (row == OPTIONS_ROW)
			{
				// only setting value for first option right now
				Measurement m = (Measurement)super.getValueAt(MEASUREMENTS_ROW, column);
				List<Option> options = m.get_options();
				if (options != null && options.size() > 0)
				{
					options.get(0).set_value(value.toString());
				}
			}
		}
		
		public Vector getColumnIdentifiers() 
		{
			return columnIdentifiers;
		}
	}
	
// Removes the specified column from the table and the associated
// call data from the table model.
	public void removeColumnAndData(JTable table, int vColIndex) 
	{
		AnalysisTableModel model = (AnalysisTableModel)table.getModel();
		TableColumn col = table.getColumnModel().getColumn(vColIndex);
		int columnModelIndex = col.getModelIndex();
		Vector data = model.getDataVector();
		Vector colIds = model.getColumnIdentifiers();
	
	// Remove the column from the table
		table.removeColumn(col);
	
	// Remove the column header from the table model
		colIds.removeElementAt(columnModelIndex);
	
	// Remove the column data
		for (int r=0; r<data.size(); r++) {
		Vector row = (Vector)data.get(r);
		row.removeElementAt(columnModelIndex);
		}
		model.setDataVector(data, colIds);
	
	// Correct the model indices in the TableColumn objects
	// by decrementing those indices that follow the deleted column
		Enumeration<TableColumn> tcEnum = table.getColumnModel().getColumns();
		for (; tcEnum.hasMoreElements(); ) {
			TableColumn c = tcEnum.nextElement();
			if (c.getModelIndex() >= columnModelIndex) {
			c.setModelIndex(c.getModelIndex()-1);
			}
		}
		model.fireTableStructureChanged();
	}

	private Measurement getMeasurementFromColumn (JTable table, int columnIdx)
	{
		return (Measurement)table.getValueAt(MEASUREMENTS_ROW, columnIdx);
	}
	
	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - Lawrence Bunch
		ResourceBundle bundle = ResourceBundle.getBundle("nlk.analysisTool.analysisTool");
		selectCmapsPanel = new JPanel();
		label1 = new JLabel();
		folderPathTextField = new JTextField();
		folderBrowseButton = new JButton();
		separator1 = new JSeparator();
		analysisPanel = new JPanel();
		analysisScrollPane = new JScrollPane();
		analysisTable = new JTable();
		resultsPanel = new JPanel();
		label6 = new JLabel();
		outputFormatPanel = new JPanel();
		label8 = new JLabel();
		outputFormatPopup = new JPopupMenu();
		menuItem1 = new JMenuItem();
		outputPanel = new JPanel();
		label7 = new JLabel();
		outputLocation = new JTextField();
		outputBrowseButton = new JButton();
		runAnalysisButton = new JButton();
		analysisHeaderPanel = new JPanel();
		label3 = new JLabel();
		label5 = new JLabel();
		measuresPanel = new JPanel();
		measuresScrollPane = new JScrollPane();
		measuresTree = new JDragTree();
		measuresHeaderPanel = new JPanel();
		label2 = new JLabel();
		label4 = new JLabel();

		//======== this ========

		setBorder(new javax.swing.border.CompoundBorder(
			new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0),
				"", javax.swing.border.TitledBorder.CENTER,
				javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font("Dialog", java.awt.Font.BOLD, 14),
				java.awt.Color.red), getBorder())); addPropertyChangeListener(new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent e){if("border".equals(e.getPropertyName()))throw new RuntimeException();}});

		setLayout(new BorderLayout());

		//======== selectCmapsPanel ========
		{
			selectCmapsPanel.setLayout(new BorderLayout());

			//---- label1 ----
			label1.setText(bundle.getString("AnalysisPanel.label1.text"));
			label1.setFont(new Font(UI_FONT, Font.ITALIC, 12));
			selectCmapsPanel.add(label1, BorderLayout.NORTH);
			selectCmapsPanel.add(folderPathTextField, BorderLayout.CENTER);

			//---- folderBrowseButton ----
			folderBrowseButton.setText(bundle.getString("AnalysisPanel.folderBrowseButton.text"));
			folderBrowseButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					folderBrowseButtonActionPerformed(e);
				}
			});
			selectCmapsPanel.add(folderBrowseButton, BorderLayout.EAST);
			selectCmapsPanel.add(separator1, BorderLayout.SOUTH);
		}
		add(selectCmapsPanel, BorderLayout.NORTH);

		//======== analysisPanel ========
		{
			analysisPanel.setLayout(new BorderLayout());
			analysisPanel.setPreferredSize(new Dimension(800, 245));
			analysisPanel.setMinimumSize(new Dimension(200, 245));
			//======== analysisScrollPane ========
			{
				//---- analysisTable ----
				//16DnD analysisTable.setDropMode(DropMode.INSERT_COLS);
			   analysisTable.setDragEnabled(true);
			   
				analysisTable.setModel(new AnalysisTableModel(
					new Object[][] {
							{"Measurement"},
							{"Label"},
							{"References"},
							{"Options"},
							{"Actions"}
					},
					new String[] {"Measurement"}
				));
				// hide the first row containing the Measurement object
				analysisTable.setRowHeight(20);
				analysisTable.setRowHeight(MEASUREMENTS_ROW, 1);
				//analysisTable.getTableHeader().setDefaultRenderer(new AnalysisCellRenderer());
				analysisTable.getColumnModel().getColumn(INFO_COLUMN).setPreferredWidth(100);
				analysisTable.getColumnModel().getColumn(INFO_COLUMN).setResizable(true);
				analysisTable.getColumnModel().setColumnSelectionAllowed(true);
				analysisTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				
				AnalysisTableTransferHandler transferHandler = new AnalysisTableTransferHandler(analysisTable);
				analysisTable.setTransferHandler(transferHandler);
				analysisTable.setDragEnabled(true);
				
				analysisTable.setAutoCreateColumnsFromModel(false);
				DropTargetListener dropListener = new DropTargetListener(){

               public void dragEnter(DropTargetDragEvent dtde) {
               }
               public void dragExit(DropTargetEvent dte) {
                }
               public void dragOver(DropTargetDragEvent dtde) {
               }
               public void drop(DropTargetDropEvent dtde) {
                  Transferable trans = dtde.getTransferable();
                  try {
                     MeasurementList measureList = (MeasurementList)trans.getTransferData(MeasurementList.getDataFlavor());
                     int col = analysisTable.columnAtPoint(dtde.getLocation());
                     if (col < analysisTable.getColumnCount())
                     {
                        col++;
                     }
                     AnalysisTableTransferHandler th = (AnalysisTableTransferHandler)analysisTable.getTransferHandler();
                     for (int m = 0; m < measureList.size(); m++)
                     {    
                        Measurement measure = new Measurement(measureList.get(m));
                        col = th.addMeasureColumn(measure,col);
                     }
                     dtde.acceptDrop(DnDConstants.ACTION_LINK);
                     dtde.dropComplete(true);
                  } catch (UnsupportedFlavorException e) {
                     // TODO Auto-generated catch block
                	  logger.log(Level.WARNING, "", e);
                  } catch (IOException e) {
                     // TODO Auto-generated catch block
                	  logger.log(Level.WARNING, "", e);
                  }
               }
               public void dropActionChanged(DropTargetDragEvent dtde) {
               }
               
            };
            
            DropTarget tableColDropTarget = new DropTarget(analysisTable,DnDConstants.ACTION_LINK,dropListener);
            analysisTable.setDropTarget(tableColDropTarget);
            
            
				analysisScrollPane.setViewportView(analysisTable);
				analysisScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
				analysisScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
			}
			analysisPanel.add(analysisScrollPane, BorderLayout.CENTER);

			//======== resultsPanel ========
			{
				resultsPanel.setLayout(new GridBagLayout());
				((GridBagLayout)resultsPanel.getLayout()).columnWidths = new int[] {247, 0, 0};
				((GridBagLayout)resultsPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0};
				((GridBagLayout)resultsPanel.getLayout()).columnWeights = new double[] {1.0, 1.0, 0};
				((GridBagLayout)resultsPanel.getLayout()).rowWeights = new double[] {1.0, 0.0, 1.0, 1.0, 0};

				//---- label6 ----
				label6.setText(bundle.getString("AnalysisPanel.label6.text"));
				label6.setFont(new Font(UI_FONT, Font.BOLD, 14));
				resultsPanel.add(label6, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));

				//======== outputFormatPanel ========
				{
					outputFormatPanel.setLayout(new BorderLayout(5, 5));
					outputFormatPanel.setPreferredSize(new Dimension(200, 20));
					//---- label8 ----
					label8.setText(bundle.getString("AnalysisPanel.label8.text"));
					outputFormatPanel.add(label8, BorderLayout.WEST);
					
					JLabel excelFormat = new JLabel ("Microsoft Excel Spreadsheet (xls)");
					outputFormatPanel.add(excelFormat, BorderLayout.CENTER);
					
//					//======== outputFormatPopup ========
//					{
//						//---- menuItem1 ----
//						menuItem1.setText(bundle.getString("AnalysisPanel.menuItem1.text"));
//						outputFormatPopup.add(menuItem1);
//						outputFormatPopup.setVisible(true);
//						outputFormatPopup.setEnabled(true);
//					}
//					outputFormatPanel.add(outputFormatPopup, BorderLayout.CENTER);
				}
				resultsPanel.add(outputFormatPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));
				//======== outputPanel ========
				{
					outputPanel.setLayout(new BorderLayout(5, 0));

					//---- label7 ----
					label7.setText(bundle.getString("AnalysisPanel.label7.text"));
					outputPanel.add(label7, BorderLayout.WEST);
					outputPanel.add(outputLocation, BorderLayout.CENTER);

					//---- outputBrowseButton ----
					outputBrowseButton.setText(bundle.getString("AnalysisPanel.outputBrowseButton.text"));
					outputBrowseButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							outputBrowseButtonActionPerformed(e);
						}
					});
					outputPanel.add(outputBrowseButton, BorderLayout.EAST);
				}
				resultsPanel.add(outputPanel, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));

				//---- runAnalysisButton ----
				runAnalysisButton.setText(bundle.getString("AnalysisPanel.runAnalysisButton.text"));
				runAnalysisButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						runAnalysisButtonActionPerformed(e);
					}
				});
				resultsPanel.add(runAnalysisButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.BOTH,
					new Insets(0, 0, 0, 0), 0, 0));
			}
			analysisPanel.add(resultsPanel, BorderLayout.SOUTH);

			//======== analysisHeaderPanel ========
			{
				analysisHeaderPanel.setLayout(new BorderLayout());

				//---- label3 ----
				label3.setText(bundle.getString("AnalysisPanel.label3.text"));
				label3.setFont(new Font(UI_FONT, Font.ITALIC, 12));
				analysisHeaderPanel.add(label3, BorderLayout.NORTH);

				//---- label5 ----
				label5.setText(bundle.getString("AnalysisPanel.label5.text"));
				label5.setFont(new Font(UI_FONT, Font.BOLD, 14));
				analysisHeaderPanel.add(label5, BorderLayout.SOUTH);
			}
			analysisPanel.add(analysisHeaderPanel, BorderLayout.NORTH);
		}
		JSplitPane leftRightSplitPane = new JSplitPane();
		leftRightSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		leftRightSplitPane.setRightComponent(analysisPanel);
		leftRightSplitPane.setResizeWeight(1);

		//======== measuresPanel ========
		{
			measuresPanel.setLayout(new BorderLayout());
			measuresPanel.setPreferredSize(new Dimension(800, 250));
			//======== measuresScrollPane ========
			{

				//---- measuresTree ----
				try {
					measuresTree.setModel(Measurement.createMeasuresTreeModel(measurementsDirectoryPath));
					MeasureTreeTransferHandler transferHandler = new MeasureTreeTransferHandler();
					measuresTree.setTransferHandler(transferHandler);
					measuresTree.setDragEnabled(true);
				} catch (FileNotFoundException e1) {
					logger.log(Level.WARNING, "", e1);
				}
				measuresScrollPane.setViewportView(measuresTree);
			}
			measuresPanel.add(measuresScrollPane, BorderLayout.CENTER);

			//======== measuresHeaderPanel ========
			{
				measuresHeaderPanel.setLayout(new BorderLayout());

				//---- label2 ----
				label2.setText(bundle.getString("AnalysisPanel.label2.text"));
				label2.setFont(new Font(UI_FONT, Font.ITALIC, 12));
				measuresHeaderPanel.add(label2, BorderLayout.NORTH);

				//---- label4 ----
				label4.setText(bundle.getString("AnalysisPanel.label4.text"));
				label4.setFont(new Font(UI_FONT, Font.BOLD, 14));
				measuresHeaderPanel.add(label4, BorderLayout.SOUTH);
			}
			measuresPanel.add(measuresHeaderPanel, BorderLayout.NORTH);
		}
		leftRightSplitPane.setLeftComponent(measuresPanel);
		add(leftRightSplitPane, BorderLayout.CENTER);
		//add(measuresPanel, BorderLayout.CENTER);
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	public class JDragTree extends JTree
	{
		private int mouseButtonDown = MouseEvent.NOBUTTON; 
		private long mouseButtonDownSince = 0; 
		
		public JDragTree()
		{
			super();
			initCustomDragGestureRecognizer();
		}
		private void initCustomDragGestureRecognizer() 
		{ 
			addMouseListener(new MouseAdapter() 
			{ 
				public void mousePressed(MouseEvent e) 
				{ 
					mouseButtonDownSince = System.currentTimeMillis(); 
					mouseButtonDown = e.getButton(); 
				} 
				public void mouseReleased(MouseEvent e) 
				{ 
					mouseButtonDown = MouseEvent.NOBUTTON; 
				} 
			}); 
			addMouseMotionListener(new MouseMotionAdapter() 
			{ 
				public void mouseDragged(MouseEvent e) 
				{ 
					if (mouseButtonDown != MouseEvent.NOBUTTON) 
					{ 
						if (getPathForLocation(e.getX(), e.getY()) != null) 
							getTransferHandler().exportAsDrag(JDragTree.this, e, DnDConstants.ACTION_COPY); 
					} 
					else 
						mouseButtonDown = MouseEvent.NOBUTTON; 
				} 
			}); 
		} 
	}
	/**
	 * @param table
	 * @param value
	 * @param column
	 */
	private JButton createReferenceButton(JTable table, Object value, int column) {
		Measurement m = (Measurement)table.getValueAt(MEASUREMENTS_ROW, column);
		if (m.get_references() != null && m.get_references().size() > 0)
		{
			String buttonLabel = "Select...";
			String valStr = value.toString();
			if (valStr.length() > 0)
			{
				int lastSlash = valStr.lastIndexOf(File.separatorChar);
				lastSlash++;
				buttonLabel = valStr.substring(lastSlash);
			}
			JButton selectRefButton = new JButton(buttonLabel);
			Reference r = m.get_references().get(0);
			selectRefButton.addActionListener(new ReferenceActionListener(r, table));
			return selectRefButton;
		}
		return null;
	}


	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - Lawrence Bunch
	private JPanel selectCmapsPanel;
	private JLabel label1;
	private JTextField folderPathTextField;
	private JButton folderBrowseButton;
	private JSeparator separator1;
	private JPanel analysisPanel;
	private JScrollPane analysisScrollPane;
	private JTable analysisTable;
	private JPanel resultsPanel;
	private JLabel label6;
	private JPanel outputFormatPanel;
	private JLabel label8;
	private JPopupMenu outputFormatPopup;
	private JMenuItem menuItem1;
	private JPanel outputPanel;
	private JLabel label7;
	private JTextField outputLocation;
	private JButton outputBrowseButton;
	private JButton runAnalysisButton;
	private JPanel analysisHeaderPanel;
	private JLabel label3;
	private JLabel label5;
	private JPanel measuresPanel;
	private JScrollPane measuresScrollPane;
	private JDragTree measuresTree;
	private JPanel measuresHeaderPanel;
	private JLabel label2;
	private JLabel label4;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
