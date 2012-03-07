/*
 * The HRT Project.
 * This work is licensed under the 
 * Creative Commons Attribution-NonCommercial 3.0 Unported License. 
 * To view a copy of this license, 
 * visit http://creativecommons.org/licenses/by-nc/3.0/ 
 * or send a letter to 
 * Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
 */
package org.hrva.capture;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Writes a CSV-formatfile using a Map<String,String> structure to provide
 * column names and values.
 * 
 * <p>
 * This uses the "quotes-optional" and "Unix newline" dialect of CSV.
 * </p>
 * 
 * <p>Here's a typical use case.</p>
 * <code><pre>
 *      String[] headings = { "Column 1", "Column 2" };
 *      Writer target= new FileWriter( some_file );
 *      CSVWriter csvwtr = new CSVWriter(target, headings);
 *      csvwtr.writeheading();
 *      Map&lt;String, String&gt; csv;
 *      csv = build_map_from_source(line);
 *      csvwtr.writerow(csv);
 *      csvwtr.close();
 * </pre></code>
 * 
 * 
 * @author slott
 */
public class CSVWriter {
    
    String[] columns;
    Writer wtr;
    
    /**
     * Opens the writer with a list of columns to use for the heading.
     * Any additional values on a given row are silently ignored.
     * 
     * @param wtr A writer which will be used to write CSV output.
     * @param columns An array of column names.
     */
    public CSVWriter( Writer wtr, String[] columns ) {
        super();
        this.columns= columns;
        this.wtr= wtr;
    }
    
    /**
     * Writes a single escaped value to the CSV file.
     * This adds quotes for special cases and doubles internal quotes.
     * 
     * @param value A String to quote if necessary.
     * @throws IOException
     */
    public void escape( String value ) throws IOException {
        if( value.contains("\"") || value.contains(",") || value.contains("\n") ) {
            wtr.write("\"");
            wtr.write( value.replace("\"","\"\"") );
            wtr.write("\"");
        }
        else {
            wtr.write(value);
        }
    }
    
    /**
     * Writes the heading row to the file.
     * 
     * @throws IOException
     */
    public void writeheading( ) throws IOException {
        String separator= "";
        for( String label : columns ) {
            wtr.write(separator);
            escape( label );
            separator= ",";
        }
        wtr.write( "\n" );
    }
    
    /**
     * Writes a row to the file.
     * @param row A Map<String,String> to map Headings To Values.
     * @throws IOException
     */
    public void writerow( Map<String,String> row ) throws IOException {
        String separator= "";
        for( String label : columns ) {
            wtr.write(separator);
            if( row.containsKey(label)) {
                escape( row.get(label) );
            }
            separator= ",";
        }
        wtr.write( "\n" );      
    }
    
}
