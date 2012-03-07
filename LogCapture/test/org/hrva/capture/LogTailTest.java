/*
 * The HRT Project.
 * Aavailable under a Creative Commons 2.0 License.
 */
package org.hrva.capture;

import java.io.*;
import java.util.Properties;
import junit.framework.TestCase;

/**
 * Tests LogTail.
 * 
 * @author slott
 */
public class LogTailTest extends TestCase {
    
    File sample_log_file;
    Writer sample_log;
    Properties shared= null;

    /**
     * Constructs TestCase instance.
     * @param testName
     */
    public LogTailTest(String testName) {
        super(testName);
    }
    
    /**
     * TestCase setup.  Removes old files.  Creates a sample log file,
     * which is left open for writing.
     * 
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File output;
        output = new File("test/log_tail_new.state");
        output.delete();
        output = new File("test/sample.extract");
        output.delete();
        sample_log_file = new File("test/sample.log");
        sample_log= new BufferedWriter( new FileWriter( sample_log_file, false ) );
        sample_log.write( "07:04:42 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842 [Valid] Adher:-1 [Valid] Odom:2668 [Valid] DGPS:On FOM:2\n" );
        sample_log.write( "07:04:42 02/15 V.1.2236 H.0.0 MT_TIMEPOINTCROSSING Time:07:04:36 Arrival Rte:4 Dir:2 TP:329 Stop:45 Svc:1 Blk:221 Lat/Lon:370315618/-763461352 [Valid] Adher:2 [Valid] Odom:1924 [Valid] DGPS:On FOM:2\n" );
        sample_log.flush();
        shared= new Properties();
    }
    
    /**
     * TestCase Teardown.  Closes the open log file.
     * @throws Exception
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        sample_log.close();
        sample_log_file.delete();
    }

    /**
     * Test of run_main method, of class LogTail.
     * @throws Exception 
     */
    public void testRun_main() throws Exception {
        System.out.println("run_main");
        String[] args = { "-o", "test/sample.extract", "test/sample.log" };
        String status= "test/logtail.history";
        Properties setup= new Properties();
        setup.setProperty( "size.test/sample.log", "8" );
        setup.setProperty( "seq.test/sample.log", "2" );
        setup.store(new FileWriter( new File( status )), "Test");

        shared.setProperty("logtail.tail_status_filename", status);        
        LogTail instance = new LogTail(shared);
        instance.run_main(args);
        BufferedReader rdr= new BufferedReader( new FileReader( new File( "test/sample.extract")));
        assertEquals( " 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842 [Valid] Adher:-1 [Valid] Odom:2668 [Valid] DGPS:On FOM:2", rdr.readLine() );
        assertEquals( "07:04:42 02/15 V.1.2236 H.0.0 MT_TIMEPOINTCROSSING Time:07:04:36 Arrival Rte:4 Dir:2 TP:329 Stop:45 Svc:1 Blk:221 Lat/Lon:370315618/-763461352 [Valid] Adher:2 [Valid] Odom:1924 [Valid] DGPS:On FOM:2", rdr.readLine() );
        assertEquals( null, rdr.readLine() );
        setup= new Properties();
        setup.load(new FileReader( new File( status )));
        assertEquals( "3", setup.getProperty( "seq.test/sample.log" ) );
        assertEquals( "327", setup.getProperty( "size.test/sample.log" ) );
    }

    /**
     * Test of tail method, of class LogTail.
     * @throws Exception 
     */
    public void testTail() throws Exception {
        System.out.println("tail");
        String status= "test/logtail.history";
        Properties setup= new Properties();
        setup.setProperty( "size.test/sample.log", "8" );
        setup.setProperty( "seq.test/sample.log", "2" );
        setup.store(new FileWriter( new File( status )), "Test");
        String source = "test/sample.log";
        String target = "test/sample.extract";
        shared.setProperty("logtail.tail_status_filename", status);
        LogTail instance = new LogTail(shared);
        String expResult = target;
        String result = instance.tail(source, target);
        assertEquals(expResult, result);
        BufferedReader rdr= new BufferedReader( new FileReader( new File( "test/sample.extract")));
        assertEquals( " 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842 [Valid] Adher:-1 [Valid] Odom:2668 [Valid] DGPS:On FOM:2", rdr.readLine() );
        assertEquals( "07:04:42 02/15 V.1.2236 H.0.0 MT_TIMEPOINTCROSSING Time:07:04:36 Arrival Rte:4 Dir:2 TP:329 Stop:45 Svc:1 Blk:221 Lat/Lon:370315618/-763461352 [Valid] Adher:2 [Valid] Odom:1924 [Valid] DGPS:On FOM:2", rdr.readLine() );
        assertEquals( null, rdr.readLine() );
        setup= new Properties();
        setup.load(new FileReader( new File( status )));
        assertEquals( "3", setup.getProperty( "seq.test/sample.log" ) );
        assertEquals( "327", setup.getProperty( "size.test/sample.log" ) );
    }


    /**
     * Test of get_state method, of class LogTail.
     */
    public void testGet_state_none() {
        System.out.println("testGet_state_none");
        String name = "test/no_such.state";
        LogTail instance = new LogTail(shared);
        Properties expResult = new Properties();
        Properties result = instance.get_state(name);
        assertEquals(expResult, result);
    }

    
    /**
     * Test of get_state method, of class LogTail.
     */
    public void testGet_state_exists() {
        System.out.println("testGet_state_exists");
        String name = "test/log_tail.state";
        LogTail instance = new LogTail(shared);
        Properties expResult = new Properties();
        expResult.setProperty( "size.test/filename.log","1");
        expResult.setProperty( "seq.test/filename.log","2");
        Properties result = instance.get_state(name);
        assertEquals(expResult, result);
    }

    /**
     * Test of save_state method, of class LogTail.
     * @throws Exception 
     */
    public void testSave_state() throws Exception {
        System.out.println("save_state");
        String name = "test/log_tail_new.state";
        Properties state = new Properties();
        state.setProperty( "size.test/filename.log","2");
        state.setProperty( "seq.test/filename.log","3");
        LogTail instance = new LogTail(shared);
        instance.save_state(name, state);
        BufferedReader rdr= new BufferedReader( new FileReader( new File(name)));
        assertEquals( "#LogTail Cache", rdr.readLine() );
        rdr.readLine(); // datestamp
        assertEquals( "seq.test/filename.log=3", rdr.readLine() );
        assertEquals( "size.test/filename.log=2", rdr.readLine() );
        assertEquals( null, rdr.readLine() );        
    }
}
