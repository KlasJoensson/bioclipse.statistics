/*******************************************************************************
 * Copyright (c) 2006 Bioclipse Project
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Egon Willighagen - core API and implementation
 *******************************************************************************/
package net.bioclipse.statistics.model;

import java.io.File;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Locale;
import java.util.Scanner;
import java.util.Vector;

import net.bioclipse.
//import net.bioclipse.util.BioclipseConsole;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.views.properties.IPropertySource;

/**
 * Concept of a mathematical matrix.
 * 
 * @author egonw
 */
public class MatrixResource extends BioResource {
	
	private static final Logger logger = Bc_statisticalPlugin.getLogManager().getLogger(MatrixResource.class.toString());
	
	public static String ID = "net.bioclipse.statistics.MatrixResource";
	
	private MatrixResourcePropertySource propSource;
	
	private IMatrixImplementationResource matrixImpl;
	
	public MatrixResource(String name) {
		super(name);
	}
	
	public MatrixResource(BioResourceType type, Object obj) {
		super(type,obj);
	}

	public MatrixResource(BioResourceType type, String resString, String name) {
		super(name);
		if (getPersistedResource()==null){
			persistedResource=PersistedResource.newResource(resString); 
		}
		setDefaultResourceType(type);
	}
	
	/**
	 * Make a copy of this object and return it if it can be parsed. 
	 * Used to create new objects with a higher level taht replaces the old on parse 
	 */
	public static IBioResource newResource(BioResourceType type, Object resourceObject, String name) {

		if (resourceObject instanceof IPersistedResource) {
			IPersistedResource persRes = (IPersistedResource) resourceObject;

			boolean parentIsParsed=persRes.getBioResourceParent().isParsed();

			//This is the copy
			MatrixResource fResource= new MatrixResource(type, persRes);
			fResource.setParsed(parentIsParsed);
			fResource.setName(name);
			if (fResource.parseResource()==true){
				return fResource;
			}
			else{
				logger.error("PersistedResource:" + fResource.getName() + " could not be parsed into a MatrixResource");
				return null;
			}
			
		}	

		if (resourceObject instanceof File) {
			IPersistedResource persRes = (IPersistedResource) resourceObject;
			
			//This is the copy
			MatrixResource matResource = new MatrixResource(type, persRes);
			matResource.setName(name);
			if (matResource.parseResource() == true) {
				return matResource;
			} else {
				logger.debug("PersistedResource:" + matResource.getName() + " could not be parsed into a MatrixResource");
				return null;
			}
			
		}
		
		logger.debug("ResourceObject not a File. Discarded.");
		return null;
	}

	/**
	 * Parse the resourceString into children or parsedResource object
	 * @return
	 */
	public boolean parseResource() {
		if (!getPersistedResource().isLoaded()) return false;	//Return false if not loaded
		if (isParsed()) return true;	//Return true if already parsed

		logger.debug("Parsing the resource...");
		
		findMatrixImplementation();
		if (matrixImpl != null) {
			// OK, next step: reading the input into the matrix
			try {
				
				String matrixString = new String(getPersistedResource().getInMemoryResource());				
				matrixImpl = parseStringIntoMatrix(matrixString);
				
				//Old parser
//				read(new String(getPersistedResource().getInMemoryResource()));

				// some demo code
//				matrixImpl.newMatrix(5, 4);
//				try {
//					matrixImpl.set(4, 3, 5.0);
//				} catch (Exception e) {
//					logger.error("Could not set matrix content! " + e.getMessage(), e);
//				}
				
				if (propSource ==null){
					propSource=new MatrixResourcePropertySource(this);
				}
				propSource.addAdvancedProperties();
				
				setParsedResource(matrixImpl);
				setParsed(true);
				return true;
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		setParsed(false);
		return false;
	}
	
    private Scanner matrixScanner(String line) {
        Scanner matrixScanner = new Scanner(line).useDelimiter(",|\\s+");       
        matrixScanner.useLocale(Locale.US); // Assures decimal marker is a point
        
        return matrixScanner;
    }
    
	private IMatrixImplementationResource parseStringIntoMatrix(
			String matrixString) 
	{
		
		String[] matrixLines = matrixString.split("\\n");
		int matrixRows = matrixLines.length;
		int matrixCols = 0;
		
		if (matrixRows == 0) {
			BioclipseConsole.writeToConsole("Matrix is empty!");
			this.setSize(matrixRows, matrixCols);
			return matrixImpl;
		}
		
		//Using whitespace and "," as delimiters
		Scanner matrixScanner = matrixScanner(matrixLines[0]);
		
		//Determine number of columns
		while( matrixScanner.hasNext() )
		{
			matrixScanner.next();
			matrixCols++;
		}
		
		//Check if first row is a header, if this row contains anything but numbers it's considered a header
		matrixScanner = matrixScanner(matrixLines[0]);
		
		if( !matrixScanner.hasNextDouble() )
		{
			System.out.println("has column headers: " + matrixLines[0]);
			//Remove one row from matrixRows because one of the rows is the header
			matrixRows--;
			int index = 0;
			while( matrixScanner.hasNext()) {
				index++;
				matrixImpl.setColumnName(index, matrixScanner.next());
			}
		}
		
//		double[][] matrix = new double[matrixRows][matrixCols];
		this.setSize(matrixRows, matrixCols);
		//Read in remaining rows
		if( matrixImpl.hasColHeader() )
		{
			for(int i = 1; i < matrixLines.length; i++ )
			{
				insertRow( i, matrixLines[i]);
			}
		}
		//No column headers, read into matrix from first line
		else
		{
			for(int i = 0; i < matrixLines.length; i++ )
			{
				insertRow( i+1, matrixLines[i]);
			}
		}
		
		matrixScanner.close();
		logger.debug("Parsed matrix");		
		return matrixImpl;
	}

	//Utility method for inserting a row into the matrix (not the column header)
	private void insertRow( int row, String rowString )
	{
		Scanner matrixScanner = matrixScanner(rowString);
		
		if( !matrixScanner.hasNextDouble() ) {
			matrixImpl.setRowName(row, matrixScanner.next());
		}
		int col = 1;
		while( matrixScanner.hasNext() )
		{
			double value = matrixScanner.nextDouble();
			try {
				if (col > matrixImpl.getColumnCount()) {
					// disregard data that does not fit the matrix
					logger.error("Found more data than can fit the matrix: too few labels?");
				} else {
					matrixImpl.set(row, col, value);
				}
			} catch (Exception e) 
			{
				logger.error("Failed to insert " + value + "at row " + row + ", col: " +col );
				e.printStackTrace();
			}
			col++;
		}
		
	}

	/**
	 * For properties
	 * May be overridden by subclasses
	 */
	public Object getAdapter(Class adapter) {
		if (adapter ==IPropertySource.class){
			if (propSource ==null){
				propSource=new MatrixResourcePropertySource(this);
			}
			return propSource;
		}
		return null;
	}

	public void set(int row, int col, double value) {
		if (matrixImpl == null) return;
		try {
			matrixImpl.set(row, col, value);
		} catch (Exception e) {
			logger.error("Could not set cell value!", e);
		}
	}
	
	public double get(int row, int col) {
		if (matrixImpl == null) return 0.0;
		try {
			return matrixImpl.get(row, col);
		} catch (Exception e) {
			logger.error("Could not determine cell content!", e);
		}
		return -1.0;
	}

	public int getColumnCount() {
		if (matrixImpl == null) return 0;
		try {
			return matrixImpl.getColumnCount();
		} catch (Exception e) {
			logger.error("Could not determine the col count!");
		}
		return -1;
	}

	public int getRowCount() {
		if (matrixImpl == null) return 0;
		try {
			return matrixImpl.getRowCount();
		} catch (Exception e) {
			logger.error("Could not determine the row count!");
		}
		return -1;
	}

	public String getParsedResourceAsString() {
		Object parsedRes = this.getParsedResource();
		if (parsedRes instanceof IMatrixImplementationResource) {
			try {
				return toString((IMatrixImplementationResource)parsedRes);
			} catch (Exception e) {
				logger.error("Could not serialize the matrix to a String! " + e.getMessage(), e);
			}
		} else {
			logger.error("GetParsedResourceAsString(): Unexpected Class type: " + parsedRes.getClass().getName());
		}
		return null;
	}
	
	public boolean updateParsedResourceFromString(String resString) {
		try {
			read(resString);
		} catch (Exception e) {
			logger.error("UpdateParsedResourceFromString: Could not parse resString: " +
				e.getMessage(), e);
			return false;
		}
		return true;
	}

	public boolean save() {
		Object parseRes = this.getParsedResource();
		if (parseRes == null) {
			logger.error("Save(): Cannot save a null parsed resource!");
		} else if (parseRes instanceof IMatrixImplementationResource) {
			try {
				String result = "";
				if (matrixImpl.hasColHeader()) {
					// COLUMNS LABELS FOUND, SAVE THEM
					for (int i=0;i<matrixImpl.getColumnCount(); i++) {
						result += matrixImpl.getColumnName(i+1);
						if ((i+1)<matrixImpl.getColumnCount()) {
							result += ",";
						}
					}
					result += "\n";
				}
				result += toString((IMatrixImplementationResource) parseRes);
				byte[] byteStream = result.getBytes();
				this.getPersistedResource().setInMemoryResource(byteStream);
				this.getPersistedResource().save();
				this.setParsedResourceDirty(false);
				return true;
			} catch (Exception e) {
				logger.error("Could not serialize the matrix to a String! " + e.getMessage(), e);
			}
		} else {
			logger.error("Save(): Unexpected Class type: " + parseRes.getClass().getName());
		}
		return false;
	}

	public void unLoad(){
		super.unLoad();
		propSource.removeAdvancedProperties();
	}

	private void findMatrixImplementation() {
		// ok, find IMatrixImplementationResource's
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("net.bioclipse.statistics.matrixImplementation");
		
		if (extensionPoint != null) {
			logger.debug("Extension points provided by bc_statistics: " + extensionPoint.getUniqueIdentifier());

			IExtension[] extensions = extensionPoint.getExtensions();

			logger.debug("Found # matrix implementations: " + extensions.length);
			for (int i=0; i<extensions.length; i++) {
				logger.debug("Found extension: " + extensions[i].getClass().getName());
				
				IConfigurationElement[] configelements = extensions[i].getConfigurationElements();
				for (int j=0; j<configelements.length & matrixImpl == null; j++) {
					try {
						matrixImpl = (IMatrixImplementationResource)configelements[i].createExecutableExtension("class");
						logger.info("Took the first matrix implementation: " + matrixImpl.getClass().getName());
					} catch (Exception e) {
						logger.debug(
								"Failed to instantiate factory: "
								+ configelements[j].getAttribute("class")
								+ " in type: "
								+ extensionPoint.getUniqueIdentifier()
								+ " in plugin: "
								+ configelements[j]
								                 .getDeclaringExtension().getExtensionPointUniqueIdentifier()
								                 , e);
					}
				}
			}
//			setParsedResource(matrixImpl);
		} else {
			BioclipseConsole.writeToConsole("No matrix implementations found!");
			logger.error("No matrix implementations found!");
		}		
	}

	private String toString(IMatrixImplementationResource matrix) throws Exception {
		StringBuffer buffer = new StringBuffer();
		for (int row=0; row<matrix.getRowCount(); row++) {
			if (matrix.getRowName(row+1) != null) {
				buffer.append(matrix.getRowName(row+1)).append(",");
			}
			for (int col=0; col<matrix.getColumnCount(); col++) {
				buffer.append(matrix.get(row+1, col+1));
				if (col<matrix.getColumnCount()) {
					buffer.append(",");
				}
			}
			buffer.append("\n");
		}
		buffer.append("\n");
		return buffer.toString();
	}
	
	/**
	 * This method is from the Jama library.
	 * 
	 * @param input
	 */
	private void read(String input) throws Exception {
	      StreamTokenizer tokenizer = new StreamTokenizer(
	          new StringReader(input)
	      );

	      // Although StreamTokenizer will parse numbers, it doesn't recognize
	      // scientific notation (E or D); however, Double.valueOf does.
	      // The strategy here is to disable StreamTokenizer's number parsing.
	      // We'll only get whitespace delimited words, EOL's and EOF's.
	      // These words should all be numbers, for Double.valueOf to parse.

	      tokenizer.resetSyntax();
	      tokenizer.wordChars(0,255);
	      tokenizer.whitespaceChars(0, ' ');
	      tokenizer.eolIsSignificant(true);
	      Vector v = new Vector();

	      // Ignore initial empty lines
	      while (tokenizer.nextToken() == StreamTokenizer.TT_EOL);
	      if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
	    	  // OK, found an empty matrix, that's fine
	    	  BioclipseConsole.writeToConsole("File is empty: creating an null matrix.");
	    	  setSize(0,0);
	          return;
	      }
	      do {
	         v.addElement(Double.valueOf(tokenizer.sval)); // Read & store 1st row.
	      } while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);

	      int n = v.size();  // Now we've got the number of columns!
	      logger.info("Input @columns: " + n);
	      double row[] = new double[n];
	      for (int j=0; j<n; j++)  // extract the elements of the 1st row.
	         row[j]=((Double)v.elementAt(j)).doubleValue();
	      v.removeAllElements();
	      v.addElement(row);  // Start storing rows instead of columns.
	      while (tokenizer.nextToken() == StreamTokenizer.TT_WORD) {
	         // While non-empty lines
	         v.addElement(row = new double[n]);
	         int j = 0;
	         do {
	            if (j >= n) throw new java.io.IOException
	               ("Row " + v.size() + " is too long.");
	            row[j++] = Double.valueOf(tokenizer.sval).doubleValue();
	         } while (tokenizer.nextToken() == StreamTokenizer.TT_WORD);
	         if (j < n) throw new java.io.IOException
	            ("Row " + v.size() + " is too short.");
	      }
	      int m = v.size();  // Now we've got the number of rows.
	      logger.info("Input @rows: " + m);

	      double[][] A = new double[m][];
	      v.copyInto(A);  // copy the rows out of the vector
	      
	      setSize(m,n);
	      for (int i=0; i<m; i++) { // loop over rows
	    	  for (int j=0; j<n; j++) { // loop over columns
	    		  matrixImpl.set(i+1, j+1, A[i][j]);
	    	  }
	      }
	      
	      logger.debug("Done reading...");
	}
	
	public void setSize(int row, int col) {
		if (matrixImpl == null) findMatrixImplementation();
		matrixImpl = matrixImpl.getInstance(row, col);
		setParsedResource(matrixImpl);
		setParsed(true);
	}
	
	@Override
	public String[] getEditorIDs(){
		String[] editors = new String[2];
		editors[0] = "net.bioclipse.editors.MatrixGridEditor";
		editors[1] = "net.bioclipse.editors.TextEditor";
		return editors;
	}

	public boolean hasRowHeader()
	{
		return matrixImpl.hasRowHeader();
	}
	
	public boolean hasColHeader()
	{
		return matrixImpl.hasColHeader();
	}

	public String getColumnName(int index) {
		return matrixImpl.getColumnName(index);
	}

	public String getRowName(int index) {
		return matrixImpl.getRowName(index);
	}

	public void setColumnName(int index, String name) {
		matrixImpl.setColumnName(index, name);
	}

	public void setRowName(int index, String name) {
		matrixImpl.setRowName(index, name);
	}
	
	
}