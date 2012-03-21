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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Combination "Wrapper" which cycles, performing a worker task.
 * 
 * <p>The two alternative workers are:</p>
 * <ul><li>{@link org.hrva.capture.Tail_Format_Push}.</li>
 * <li>{@link org.hrva.capture.Tail_Only}.  This is the default.</li>
 * </ul>
 *
 * <p>By default, this cycles on a one-minute interval.  It can be
 * set to run on-time-only, also.</p>
 *
 * <p>This is both a main program with a command-line interface, as well as
 * object that can be used to push. </p>
 *
 * <p>Typical use case</p>
 * <code><pre>
 *      File prop_file = new File("hrtail.properties");
 *      Properties config= new Properties();
 *      config.load(new FileInputStream(prop_file));
 *
 *      Capture instance = new Capture(config);
 *      CaptureWorker worker = new Tail_Format_Push();
 *      instance.capture( worker, 60.0, "/path/to/spysocket.log" );
 * </pre></code>
 *
 * <p>The two alternative workers are:</p>
 * <ul><li>{@link org.hrva.capture.Tail_Format_Push}.</li>
 * <li>{@link org.hrva.capture.Tail_Only}.  This is the default.</li>
 * </ul>
 *
 * <p>At the command line, it might look like this.</p>
 * <code><pre>
 * java -cp LogCapture/dist/LogCapture.jar org.hrva.capture.Capture -t Tail_Format_Push /path/to/spysocket.log
 * </pre></code>
 *
 * <p>This will cycle indefinitely, capturing, reformatting and pushing extracts
 * from the log file. </p>
 *
 * <p>This uses the <tt>hrtail.properties</tt> file.</p> <dl>
 * <dt><tt>capture.extract_filename</tt><dd>The file to which to write log
 * extracts</dd> <dt><tt>capture.csv_filename</tt><dd>The file to which to write
 * reformatted extracts</dd> </dl>
 *
 * @author slott
 */
public class Capture {

    /**
     * Properties for this application.
     */
    Properties global;
    /**
     * The Scheduling timer.
     */
    Timer timer = new Timer();
    /**
     * Logger.
     */
    final Log logger = LogFactory.getLog(Capture.class);

    /**
     * Command-line program to tail a log and then push file to the HRT couch
     * DB.
     *
     * <p>All this does is read properties and invoke run_main</p>
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        Log log = LogFactory.getLog(Reformat.class);
        File prop_file = new File("hrtail.properties");
        Properties config = new Properties();
        try {
            config.load(new FileInputStream(prop_file));
        } catch (IOException ex) {
            log.warn("Can't find " + prop_file.getName(), ex);
            try {
                log.debug(prop_file.getCanonicalPath());
            } catch (IOException ex1) {
            }
        }
        Capture capture = new Capture(config);
        try {
            capture.run_main(args);
        } catch (ParseException ex1) {
            log.fatal("Invalid Options", ex1);
        } catch (MalformedURLException ex2) {
            log.fatal("Invalid CouchDB URL", ex2);
        } catch (IOException ex3) {
            log.fatal(ex3);
        }
    }

    /**
     * Build the LogTail instance.
     *
     * @param global The hrtail.properties file
     */
    public Capture(Properties global) {
        super();
        this.global = global;
    }

    /**
     * Does the three-stop process of tail, reformat and couch push.
     *
     * <p>Accepts the following options</p> <dl> <dt><tt>-1</tt></dt><dd>Don't
     * cycle; process once only.</dd> <dt><tt>-v</tt></dt><dd>Verbose
     * logging.</dd> </dl> <p>This gets properties from the
     * <tt>hrtail.properties</tt> file.</p>
     *
     *
     * @param args the command line arguments
     * @throws ParseException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void run_main(String[] args) throws ParseException, FileNotFoundException, IOException {
        Option verbose = new Option("v", "verbose", false, "Verbose logging");
        Option one_time = new Option("1", "onetime", false, "One time only; do not cycle");
        Option task = OptionBuilder.withArgName("task").hasArgs(1).withDescription("Task Class Name, either 'Tail_Format_Push' or 'Tail_Only'").create("t");
        Option cycle_time = OptionBuilder.withArgName("cycle_time").hasArgs(1).withDescription("Cycle Time in seconds").create("c");

        Options options = new Options();
        options.addOption(verbose);
        options.addOption(one_time);
        options.addOption(task);
        options.addOption(cycle_time);

        CommandLineParser parser = new GnuParser();
        CommandLine line = null;

        CaptureWorker worker;
        List positional_args;
        double time_value = 60.0;
        try {
            // parse the command line arguments
            line = parser.parse(options, args);
            positional_args = line.getArgList();
            if (positional_args.size() != 1) {
                throw new ParseException("Only one log file can be captured");
            }
            if (line.hasOption("1")) {
                time_value = 0.0;
            } else {
                double value;
                if (line.hasOption("c")) {
                    value = Double.parseDouble(line.getOptionValue("c"));
                } else {
                    value = 60.0;
                }
            }
            if (line.hasOption("t")) {
                String class_name = line.getOptionValue("t");
                if( ! class_name.startsWith("org.hrva.capture.")) {
                    class_name = "org.hrva.capture."+class_name;
                }
                Class wc = Class.forName(class_name);
                worker = (CaptureWorker) wc.newInstance();
            } else {
                worker = new Tail_Only();
            }
            worker.setCapture(this);

        } catch (ClassNotFoundException ex) {
            System.err.println("Parsing failed.  Reason: " + ex.getMessage());
            throw new ParseException("Unknown Task: " + line.getOptionValue("t"));
        } catch (InstantiationException ex) {
            System.err.println("Parsing failed.  Reason: " + ex.getMessage());
            throw new ParseException("Invalid use of Task: " + line.getOptionValue("t"));
        } catch (IllegalAccessException ex) {
            System.err.println("Parsing failed.  Reason: " + ex.getMessage());
            throw new ParseException("Invalid use of Task: " + line.getOptionValue("t"));
        } catch (ParseException ex) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + ex.getMessage());
            throw ex;
        }


        for (Object source : positional_args) {
            capture(worker, time_value, (String) source);
        }
    }

    /**
     * Captures an extract of a log file, reformats it and uploads it. <p>If
     * seconds is zero, this is run once.</p> <p>If seconds is non-zero, this is
     * the delay for repeat scheduling. Since a push takes almost 1 second, the
     * intervals need to be fairly long. </p>
     *
     * @param worker The CaptureWorker to use. Can be a Tail_Format_Push
     * instance or a Tail_Only instamnce
     * @param source Log File to capture, reformat and push.
     * @param seconds Scheduling interval in seconds. 0.0 means one-time-only.
     */
    public void capture(CaptureWorker worker, double seconds, String source) {
        worker.setCapture(this);
        worker.setSource_filename(source);
        worker.setExtract_filename(global.getProperty("capture.extract_filename", "hrtrtf.txt"));
        worker.setCsv_filename(global.getProperty("capture.csv_filename", "hrtrtf.csv"));
        if (seconds == 0.0) {
            timer.schedule(worker, 2 * 1000);
        } else {
            long interval = (long) seconds * 1000;
            long current = Calendar.getInstance().get(Calendar.SECOND);
            timer.scheduleAtFixedRate(worker, (60 - current) * 1000, interval);
        }
    }
}
