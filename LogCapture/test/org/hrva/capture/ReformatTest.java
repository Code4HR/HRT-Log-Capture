package org.hrva.capture;

import java.io.*;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.hrva.capture.Reformat.InvalidRow;

/**
 * Tests Reformat class.
 * 
 * <p>Requires the <tt>test/sample.input</tt> file.</p>
 * 
 * @author slott
 */
public class ReformatTest extends TestCase {

    Properties shared= null;
    
    /**
     * Constructs TestCase.
     * @param testName
     */
    public ReformatTest(String testName) {
        super(testName);
    }

    /**
     * Test setup.
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File output = new File("test/sample.output");
        output.delete();
        shared= new Properties();
    }

    /**
     * Test Teardown
     * @throws Exception
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of run_main method, of class Reformat.
     * @throws Exception 
     */
    public void testRun_main() throws Exception {
        System.out.println("run_main");
        String[] args = {"-o", "test/sample.output", "test/sample.input"};
        Reformat instance = new Reformat(shared);
        instance.run_main(args);
        // Read test/test.output
        BufferedReader result = new BufferedReader(new FileReader(new File("test/sample.output")));
        assertEquals("Date,Time,Vehicle,Lat,Lon,Location Valid/Invalid,Adherence,Adherence Valid/Invalid,Route,Direction,Stop", result.readLine());
        assertEquals("2012-02-15,07:04:42,V.1.2233,37.0620935,-76.3413842,V,-1,V,,,", result.readLine());
        assertEquals("2012-02-15,07:04:42,V.1.2236,37.0315618,-76.3461352,V,2,V,4,2,45", result.readLine());
        try {
            String data = result.readLine();
            assertEquals(null, data);
        } catch (EOFException ex) {
            // Acceptable, also.
        }
    }

    /**
     * Test of reformat method, of class Reformat.
     * @throws Exception 
     */
    public void testReformat_bad_1() throws Exception {
        System.out.println("testReformat_bad_1");
        Reader source = new StringReader("sample data");
        Writer target = new StringWriter();
        Reformat instance = new Reformat(shared);
        instance.include_header= true;
        instance.reformat(source, target);
        String result = "Date,Time,Vehicle,Lat,Lon,Location Valid/Invalid,Adherence,Adherence Valid/Invalid,Route,Direction,Stop\n";
        assertEquals(result, target.toString());
    }

    /**
     * Test of reformat method, of class Reformat.
     * @throws Exception 
     */
    public void testReformat_good_1() throws Exception {
        System.out.println("testReformat_good_1");
        Reader source = new StringReader("07:04:42 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842 [Valid] Adher:-1 [Valid] Odom:2668 [Valid] DGPS:On FOM:2");
        Writer target = new StringWriter();
        Reformat instance = new Reformat(shared);
        instance.include_header= true;
        instance.reformat(source, target);
        String result = "Date,Time,Vehicle,Lat,Lon,Location Valid/Invalid,Adherence,Adherence Valid/Invalid,Route,Direction,Stop\n"
                + "2012-02-15,07:04:42,V.1.2233,37.0620935,-76.3413842,V,-1,V,,,\n";
        assertEquals(result, target.toString());
    }

    /**
     * Test of label_value method, of class Reformat.
     * @throws org.hrva.hrtail.Reformat.InvalidRow 
     */
    public void testLabel_value_good() throws InvalidRow {
        System.out.println("testLabel_value_good");
        String word = "Lat/Lon:370620935/-763413842";
        String label = "Lat/Lon";
        Reformat instance = new Reformat(shared);
        String expResult = "370620935/-763413842";
        String result = instance.label_value(word, label);
        assertEquals(expResult, result);
    }

    /**
     * Test of get_lat method, of class Reformat.
     * @throws org.hrva.hrtail.Reformat.InvalidRow 
     */
    public void testGet_lat() throws InvalidRow {
        System.out.println("testGet_lat");
        String lat_lon = "370620935/-763413842";
        Reformat instance = new Reformat(shared);
        String expResult = "37.0620935";
        String result = instance.get_lat(lat_lon);
        assertEquals(expResult, result);
    }

    /**
     * Test of get_lon method, of class Reformat.
     * @throws org.hrva.hrtail.Reformat.InvalidRow 
     */
    public void testGet_lon() throws InvalidRow {
        System.out.println("testGet_lon");
        String lat_lon = "370620935/-763413842";
        Reformat instance = new Reformat(shared);
        String expResult = "-76.3413842";
        String result = instance.get_lon(lat_lon);
        assertEquals(expResult, result);
    }

    /**
     * Test of extract_fields method, of class Reformat.
     */
    public void testExtract_fields_bad_1() {
        System.out.println("testExtract_fields_bad_1");
        String line = "random junk";
        Reformat instance = new Reformat(shared);
        Map expResult = null;
        Map result = null;
        try {
            result = instance.extract_fields(line);
            fail("Should have thrown exception");
        } catch (InvalidRow ex) {
            // Expected
        }
        assertEquals(expResult, result);
    }

    /**
     * Test of extract_fields method, of class Reformat.
     */
    public void testExtract_fields_incomplete_location_1() {
        System.out.println("testExtract_fields_incomplete_location_1");
        String line = "07:04:42 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842";
        Reformat instance = new Reformat(shared);
        Map expResult = null;
        Map result = null;
        try {
            result = instance.extract_fields(line);
            fail("Should have thrown exception");
        } catch (InvalidRow ex) {
            Logger.getLogger(ReformatTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertEquals(expResult, result);
    }

    /**
     * Test of extract_fields method, of class Reformat.
     */
    public void testExtract_incomplete_location_2() {
        System.out.println("testExtract_incomplete_location_2");
        String line = "07:04:42 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842 [Valid] Adher:-1 [Valid] Odom:2668 [Valid] DGPS:On FOM:";
        Reformat instance = new Reformat(shared);
        Map expResult = null;
        Map result = null;
        try {
            result = instance.extract_fields(line);
            fail("Should have thrown exception");
        } catch (InvalidRow ex) {
            Logger.getLogger(ReformatTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertEquals(expResult, result);
    }

    /**
     * Test of extract_fields method, of class Reformat.
     */
    public void testExtract_incomplete_location_3() {
        System.out.println("testExtract_incomplete_location_3");
        String line = ":04:42 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842 [Valid] Adher:-1 [Valid] Odom:2668 [Valid] DGPS:On FOM:2";
        Reformat instance = new Reformat(shared);
        Map expResult = null;
        Map result = null;
        try {
            result = instance.extract_fields(line);
            fail("Should have thrown exception");
        } catch (InvalidRow ex) {
            Logger.getLogger(ReformatTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        assertEquals(expResult, result);
    }

    /**
     * Test of extract_fields method, of class Reformat.
     * @throws org.hrva.hrtail.Reformat.InvalidRow
     */
    public void testExtract_filter_dwell() throws InvalidRow {
        System.out.println("testExtract_filter_dwell");
        String line = "07:04:42 02/15 V.1.3515 H.0.0 MT_TIMEPOINTCROSSING Time:07:04:37 Dwell:22 Rte:65 Dir:2 TP:352 Stop:69 Svc:1 Blk:203 Lat/Lon:370425333/-764286136 [Valid] Adher:-1 [Valid] Odom:1712 [Valid] DGPS:On FOM:2";
        Reformat instance = new Reformat(shared);
        Map expResult = null;
        Map result = instance.extract_fields(line);
        assertEquals(expResult, result);
    }

    /**
     * Test of extract_fields method, of class Reformat.
     * @throws org.hrva.hrtail.Reformat.InvalidRow
     */
    public void testExtract_location() throws InvalidRow {
        System.out.println("testExtract_location");
        String line = "07:04:42 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842 [Valid] Adher:-1 [Valid] Odom:2668 [Valid] DGPS:On FOM:2";
        Reformat instance = new Reformat(shared);
        instance.now.set(Calendar.YEAR, 2012); // Force the year
        Map expResult = new TreeMap<String, String>();
        expResult.put("Date", "2012-02-15");
        expResult.put("Time", "07:04:42");
        expResult.put("Vehicle", "V.1.2233");
        expResult.put("H", "H.0.0");
        expResult.put("Lat", "37.0620935");
        expResult.put("Lon", "-76.3413842");
        expResult.put("Location Valid/Invalid", "V");
        expResult.put("Adherence", "-1");
        expResult.put("Adherence Valid/Invalid", "V");
        expResult.put("Odom", "2668");
        expResult.put("Odom Valid/Invalid", "V");
        expResult.put("DGPS", "On");
        expResult.put("FOM", "2");
        Map result = instance.extract_fields(line);
        assertEquals(expResult, result);
    }

    /**
     * Test of extract_fields method, of class Reformat.
     * @throws org.hrva.hrtail.Reformat.InvalidRow
     */
    public void testExtract_arrival() throws InvalidRow {
        System.out.println("testExtract_arrival");
        String line = "07:04:42 02/15 V.1.2236 H.0.0 MT_TIMEPOINTCROSSING Time:07:04:36 Arrival Rte:4 Dir:2 TP:329 Stop:45 Svc:1 Blk:221 Lat/Lon:370315618/-763461352 [Valid] Adher:2 [Valid] Odom:1924 [Valid] DGPS:On FOM:2";
        Reformat instance = new Reformat(shared);
        instance.now.set(Calendar.YEAR, 2012); // Force the year
        Map expResult = new TreeMap<String, String>();
        expResult.put("Date", "2012-02-15");
        expResult.put("Time", "07:04:42");
        expResult.put("Vehicle", "V.1.2236");
        expResult.put("H", "H.0.0");
        expResult.put("Lat", "37.0315618");
        expResult.put("Lon", "-76.3461352");
        expResult.put("Location Valid/Invalid", "V");
        expResult.put("Adherence", "2");
        expResult.put("Adherence Valid/Invalid", "V");
        expResult.put("Odom", "1924");
        expResult.put("Odom Valid/Invalid", "V");
        expResult.put("DGPS", "On");
        expResult.put("FOM", "2");

        expResult.put("Arrival", "07:04:36");
        expResult.put("Route", "4");
        expResult.put("Direction", "2");
        expResult.put("TP", "329");
        expResult.put("Stop", "45");
        expResult.put("Svc", "1");
        expResult.put("Blk", "221");
        Map result = instance.extract_fields(line);
        assertEquals(expResult, result);
    }
}
