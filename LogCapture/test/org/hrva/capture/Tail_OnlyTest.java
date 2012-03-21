/*
 * The HRT Project.
 * Aavailable under a Creative Commons 2.0 License.
 */
package org.hrva.capture;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import junit.framework.TestCase;

/**
 *
 * @author slott
 */
public class Tail_OnlyTest extends TestCase {
    private File sample_log_file;
    private BufferedWriter sample_log;
    private Properties shared;
    private Capture capture;
    
    public Tail_OnlyTest(String testName) {
        super(testName);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sample_log_file = new File("test/sample.log");
        sample_log= new BufferedWriter( new FileWriter( sample_log_file, false ) );
        sample_log.write( "07:04:42 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842 [Valid] Adher:-1 [Valid] Odom:2668 [Valid] DGPS:On FOM:2\n" );
        sample_log.write( "07:04:42 02/15 V.1.2236 H.0.0 MT_TIMEPOINTCROSSING Time:07:04:36 Arrival Rte:4 Dir:2 TP:329 Stop:45 Svc:1 Blk:221 Lat/Lon:370315618/-763461352 [Valid] Adher:2 [Valid] Odom:1924 [Valid] DGPS:On FOM:2\n" );
        sample_log.flush();
        File ext= new File( "test/capture.txt" );
        ext.delete();
        File history= new File( "test/capture.history" );
        history.delete();
        shared= new Properties();
        shared.setProperty("capture.extract_filename", "test/capture.txt");
        shared.setProperty("capture.csv_filename", "test/capture.csv");
        shared.setProperty("logtail.tail_status_filename","test/capture.history");
        shared.setProperty("couchpush.db_url","http://localhost:5984/couchdbkit_test");
        capture= new Capture(shared);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        sample_log.close();
        //sample_log_file.delete();
    }

    /**
     * Test of run method, of class Tail_Only.
     */
    public void testRun() throws Exception {
        System.out.println("run");
        Tail_Only instance = new Tail_Only(capture);
        instance.setSource_filename("test/sample.log");
        instance.setExtract_filename("test/capture.txt");
        instance.run();
        
        File ext= new File( "test/capture.txt" );
        assertEquals( 327, ext.length() );
        File history= new File( "test/capture.history" );
        Properties status= new Properties();
        status.load( new FileReader(history) );
        assertEquals( "327", status.getProperty("size.test/sample.log") );
        assertEquals( "1", status.getProperty("seq.test/sample.log") );
    }

    /**
     * Test of setCsv_filename method, of class Tail_Only.
     */
    public void testSetCsv_filename() {
        System.out.println("setCsv_filename");
        String csv_filename = "test/capture.csv";
        Tail_Only instance = new Tail_Only(capture);
        instance.setCsv_filename(csv_filename);
        assertEquals( null, instance.extract_filename );
    }

    /**
     * Test of setExtract_filename method, of class Tail_Format_Push.
     */
    public void testSetExtract_filename() {
        System.out.println("setExtract_filename");
        String extract_filename = "test/capture.txt";
        Tail_Only instance = new Tail_Only(capture);
        instance.setExtract_filename(extract_filename);
        assertEquals( extract_filename, instance.extract_filename );
    }

    /**
     * Test of setSource_filename method, of class Tail_Format_Push.
     */
    public void testSetSource_filename() {
        System.out.println("setSource_filename");
        String source_filename = "test/sample.log";
        Tail_Only instance = new Tail_Only(capture);
        instance.setSource_filename(source_filename);
        assertEquals( source_filename, instance.source_filename );
    }

}
