CmapAnalysis is a cross-platform application that enables users to define and execute analyses over sets of concept maps. The result of the analysis is a Microsoft Excel spreadsheet (XML) containing one row for each concept map in the analyzed set with columns for each of the desired measures. Measures range from simple counts of concepts and
propositions to more complex calculations such as identifying the top three most central concepts in each map.

CmapAnalysis was designed with several objectives in mind:
  * CmapAnalysis should support Size, Quality and Structure measures in the analysis of concept maps, as described above.
  * CmapAnalysis should take as input Cmaps in the open CXL file format in addition to the .cmap format, allowing the analysis of concept maps developed by concept mapping programs that utilize CXL.
  * CmapAnalysis can handle large sets of concept maps, calculating the measures for all the maps in the given set.
  * In addition to the analysis measures incorporated into the program described below, CmapAnalysis should be extensible, meaning that users (with technical inclination) should be able to add other measures to the program.
  * The results provided by the program should be in a format that lends to further analysis (e.g. XML format of a MS Excel spreadsheet).