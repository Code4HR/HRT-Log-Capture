package org.hrva.capture;

import java.io.StringWriter;
import java.util.Map;
import java.util.TreeMap;
import junit.framework.TestCase;

/**
 * Tests CSVWriter.
 * 
 * @author slott
 */
public class CSVWriterTest extends TestCase {
    
    StringWriter buffer;
    String[] headings = { "Col1", "Col2" };
    
    /**
     * Constructs the TestCase.
     * 
     * @param testName
     */
    public CSVWriterTest(String testName) {
        super(testName);
    }
    
    /**
     * TestCase setUp
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        buffer= new StringWriter();
        
    }
    
    /**
     * TestCase tearDown
     * @throws Exception
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of escape method, of class CSVWriter.
     * @throws Exception 
     */
    public void testEscape_simple() throws Exception {
        System.out.println("escape simple");
        String value = "word";
        CSVWriter instance = new CSVWriter(buffer, headings);
        instance.escape(value);
        assertEquals( "word", buffer.toString() );
    }
    
    /**
     * Test of escape method, of class CSVWriter.
     * @throws Exception 
     */
    public void testEscape_quote() throws Exception {
        System.out.println("escape quote");
        String value = "contains \"quotes\"";
        CSVWriter instance = new CSVWriter(buffer, headings);
        instance.escape(value);
        assertEquals( "\"contains \"\"quotes\"\"\"", buffer.toString() );
    }

    /**
     * Test of escape method, of class CSVWriter.
     * @throws Exception 
     */
    public void testEscape_comma() throws Exception {
        System.out.println("escape comma");
        String value = "contains, comma";
        CSVWriter instance = new CSVWriter(buffer, headings);
        instance.escape(value);
        assertEquals( "\"contains, comma\"", buffer.toString() );
    }

    /**
     * Test of writeheading method, of class CSVWriter.
     * @throws Exception 
     */
    public void testWriteheading() throws Exception {
        System.out.println("writeheading");
        CSVWriter instance = new CSVWriter(buffer, headings);
        instance.writeheading();
        assertEquals( "Col1,Col2\n", buffer.toString() );
    }

    /**
     * Test of writerow method, of class CSVWriter.
     * @throws Exception 
     */
    public void testWriterow_extra() throws Exception {
        System.out.println("testWriterow_extra");
        Map<String, String> row = new TreeMap<String,String>();
        row.put("Col1","data1");
        row.put("Col2","data,2");
        row.put("Col3","extra");
        CSVWriter instance = new CSVWriter(buffer, headings);
        instance.writerow(row);
        assertEquals( "data1,\"data,2\"\n", buffer.toString() );
    }

    /**
     * Test of writerow method, of class CSVWriter.
     * @throws Exception 
     */
    public void testWriterow_missing() throws Exception {
        System.out.println("testWriterow_missing");
        Map<String, String> row = new TreeMap<String,String>();
        row.put("Col1","data1");
        row.put("Col3","extra");
        CSVWriter instance = new CSVWriter(buffer, headings);
        instance.writerow(row);
        assertEquals( "data1,\n", buffer.toString() );
    }
}
