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

import java.io.*;
import java.util.Properties;
import junit.framework.TestCase;

/**
 * Tests Capture application, which knits together LogTail, Reformat
 * and CouchPush.
 * <p>
 * This requires a couchdb server running at
 * <a href="http://localhost:5984/couchdbkit_test/">http://localhost:5984/couchdbkit_test/</a>
 * </p>
 * @author slott
 */
public class CaptureTest extends TestCase {
    
    File sample_log_file;
    Writer sample_log;
    Properties shared= null;
    
    /**
     * Constructs the TestCase.
     * @param testName
     */
    public CaptureTest(String testName) {
        super(testName);
    }
    
    /**
     * TestCase setup.
     * @throws Exception
     */
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
        File csv= new File( "test/capture.csv" );
        csv.delete();
        File history= new File( "test/capture.history" );
        history.delete();
        shared= new Properties();
        shared.setProperty("capture.extract_filename", "test/capture.txt");
        shared.setProperty("capture.csv_filename", "test/capture.csv");
        shared.setProperty("logtail.tail_status_filename","test/capture.history");
        shared.setProperty("couchpush.db_url","http://localhost:5984/couchdbkit_test");
    }
    
    /**
     * TestCase TearDown.
     * @throws Exception
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        sample_log.close();
        //sample_log_file.delete();
    }

    /**
     * Test of run_main method, of class Capture.
     * @throws Exception 
     */
    public void testRun_main() throws Exception {
        System.out.println("run_main");
        String[] args = { "-1", "test/sample.log" };
        Capture instance = new Capture(shared);
        instance.run_main(args);
        Thread.sleep(3000); // Wait
        File ext= new File( "test/capture.txt" );
        assertEquals( 327, ext.length() );
        File csv= new File( "test/capture.csv" );
        assertEquals( 231, csv.length() );
    }

    /**
     * Test of capture method, of class Capture.
     * @throws InterruptedException 
     * @throws FileNotFoundException
     * @throws IOException  
     */
    public void testCapture() throws InterruptedException, FileNotFoundException, IOException {
        System.out.println("capture");
        String source = "test/sample.log";
        double seconds = 0.0;
        
        Capture instance = new Capture(shared);
        instance.worker.push.verbose= true;
        
        instance.capture(source, seconds);
        Thread.sleep(3000); // Wait
        File ext= new File( "test/capture.txt" );
        assertEquals( 327, ext.length() );
        File csv= new File( "test/capture.csv" );
        assertEquals( 231, csv.length() );
        File history= new File( "test/capture.history" );
        Properties status= new Properties();
        status.load( new FileReader(history) );
        assertEquals( "327", status.getProperty("size.test/sample.log") );
        assertEquals( "1", status.getProperty("seq.test/sample.log") );

        instance = new Capture(shared);
        instance.capture(source, seconds);
        Thread.sleep(2500); // Wait
        status.load( new FileReader(history) );
        assertEquals( "327", status.getProperty("size.test/sample.log") );
        assertEquals( "2", status.getProperty("seq.test/sample.log") );
        
    }
}
