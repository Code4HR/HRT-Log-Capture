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
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Reformats the tail of a GPS Log file to extract essential fields.
 *
 * <p>
 * There are three formats.
 * </p>
 * 
 * <code>
 * 07:04:42 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842
 * [Valid] Adher:-1 [Valid] Odom:2668 [Valid] DGPS:On FOM:2
 * </code>
 * <br/>
 * <code>
 * 07:04:42 02/15 V.1.3515 H.0.0 MT_TIMEPOINTCROSSING Time:07:04:37 Dwell:22
 * Rte:65 Dir:2 TP:352 Stop:69 Svc:1 Blk:203 Lat/Lon:370425333/-764286136
 * [Valid] Adher:-1 [Valid] Odom:1712 [Valid] DGPS:On FOM:2
 * </code>
 * <br/>
 * <code>
 * 07:04:42 02/15 V.1.2236 H.0.0 MT_TIMEPOINTCROSSING Time:07:04:36 Arrival
 * Rte:4 Dir:2 TP:329 Stop:45 Svc:1 Blk:221 Lat/Lon:370315618/-763461352 [Valid]
 * Adher:2 [Valid] Odom:1924 [Valid] DGPS:On FOM:2
 * <code>
 * 
 * <p>
 * The output format is CSV
 * </p>
 * 
 * <code>
 * Date,Time,Vehicle,Lat/Lon,Location Valid/Invalid,Adherence,Adherence
 * Valid/Invalid,Route,Direction,Stop
 * </code>
 * 
 * <p>Typical use case</p>
 * 
 * <code><pre>
 *      File target = new File(extract_filename);
 *      Writer wtr = new FileWriter(target, true);  
 *      File source = new File(filename);
 *      Reader rdr = new FileReader(source)
 *      reformat(rdr, wtr);
 *      rdr.close();
 *      wtr.close();
 * </pre></code>
 * 
 * <p>At the command line, it might look like this.</p> 
 * <code><pre>
 * java -cp LogCapture/dist/LogCapture.jar org.hrva.capture.Reformat -o extract.csv extract.txt 
 * </pre></code>
 * 
 * 
 * @author slott
 */
public class Reformat {

    /** Properties for this application. */
     Properties global;

    /** Output file name. */
    @Option(name = "-o", usage = "Output file name.")
    String extract_filename = "hrtrtf.csv";
    /** Verbose debugging. */
    @Option(name = "-v", usage = "Vebose logging")
    boolean verbose= false;
    /** Command-line Arguments. */
    @Argument
    List<String> arguments = new ArrayList<String>();
    

    /** CSV Headings. */
    String[] headings = {
        "Date", "Time", "Vehicle", "Lat", "Lon", "Location Valid/Invalid",
        "Adherence", "Adherence Valid/Invalid", "Route", "Direction", "Stop"
    };
    
    /** Default year used to fill in incomplete dates. */
    Calendar now;
    
    /** Is a CSV header row required?  Only of the file is new. */
    boolean include_header= false;

    /** Logger. */
    final Log  logger = LogFactory.getLog(Reformat.class);

    /**
     * This row is invalid.
     */
    class InvalidRow extends Exception {

        public InvalidRow() {
            super();
        }

        public InvalidRow(String message) {
            super(message);
        }
    };

    /**
     * Command-line program to tail a log and then push file to the HRT couch
     * DB.
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
            log.warn( "Can't find "+prop_file.getName(), ex );
            try {
                log.debug(prop_file.getCanonicalPath());
            } catch (IOException ex1) {
            }
        }
        Reformat fmt = new Reformat(config);
        try {
            fmt.run_main(args);
        } catch (CmdLineException ex1) {
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
    public Reformat(Properties global) {
        super();
        this.global= global;
        // Might be overridden or updated for testability purposes.
        now = Calendar.getInstance();
    }

    /**
     * Reformats log extract file(s).  
     * 
     * <p>Each file in the command-line arguments is opened, read, reformatted
     * and written to the output CSV file.
     * </p>
     *
     * @param args the command line arguments
     * @throws CmdLineException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void run_main(String[] args) throws CmdLineException, FileNotFoundException, IOException {
        CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(args);

        File target = new File(extract_filename);
        include_header= target.length() == 0;
        Writer wtr = new FileWriter(target, true);
        try {
            for (String filename : arguments) {
                Object[] details = { filename, extract_filename };
                logger.info( MessageFormat.format("Reformatting {0} to {1}",details));

                File source = new File(filename);
                Reader rdr= new FileReader(source);
                reformat(rdr, wtr);
                rdr.close();
            }
        } finally {
            wtr.close();
        }
    }

    /**
     * Reformat a source reader to append to a source writer.
     * 
     * <p>
     * This will apply the extract_fields function to each row
     * of the reader.  If the row does not raise some kind of exception,
     * the resulting mapping is written to the output CSV-format
     * file.
     * </p>
     * 
     * @param source Reader for an input file.
     * @param target Writer for the Output file.
     * @throws IOException
     */
    public void reformat(Reader source, Writer target) throws IOException {
        CSVWriter csvwtr = new CSVWriter(target, headings);
        // Only needed once!
        if( include_header ) {
            csvwtr.writeheading();
            include_header= false;
        }

        // Note that the input file may be broken at a bad byte boundary...
        // Open input for reading and hope for the test
        BufferedReader rdr = new BufferedReader(source);
        try {
            String line = rdr.readLine();
            while (line != null) {
                try {
                    Map<String, String> csv;
                    csv = extract_fields(line);
                    if (csv == null) {
                        // filtered
                    } else {
                        csvwtr.writerow(csv);
                    }
                } catch (InvalidRow ex) {
                    logger.warn("Invalid '" + line + "'");
                }
                line = rdr.readLine();
            }

        } finally {
            rdr.close();
        }
    }

    /**
     * Split the label from the value, and confirm
     * the label as well as a non-zero length value.
     * @param word
     * @param label
     * @return
     * @throws org.hrva.hrtail.Reformat.InvalidRow 
     */
    String label_value(String word, String label) throws InvalidRow {
        String[] lv = word.split(":", 2);
        if (lv.length != 2) {
            throw new InvalidRow();
        }
        if (!lv[0].equals(label)) {
            throw new InvalidRow();
        }
        if (lv[1].length() == 0) {
            throw new InvalidRow();
        }
        return lv[1];
    }
    
    final SimpleDateFormat time_fmt = new SimpleDateFormat("HH:mm:ss");

    /**
     * Get a time value.
     * @param word
     * @return
     * @throws org.hrva.hrtail.Reformat.InvalidRow 
     */
    String get_time(String word) throws InvalidRow {
        try {
            time_fmt.parse(word);
        } catch (ParseException ex) {
            throw new InvalidRow();
        }
        return word;
    }
    
    final SimpleDateFormat input_date_fmt = new SimpleDateFormat("MM/dd");
    final SimpleDateFormat output_date_fmt = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Get a date value, converting the year to be the current year.
     * @param word
     * @return
     * @throws org.hrva.hrtail.Reformat.InvalidRow 
     */
    String get_date(String word) throws InvalidRow {
        Calendar date = Calendar.getInstance();
        try {
            date.setTime(input_date_fmt.parse(word));
        } catch (ParseException ex) {
            throw new InvalidRow();
        }
        date.set(Calendar.YEAR, now.get(Calendar.YEAR));
        return output_date_fmt.format(date.getTime());
    }

    /**
     * Get the latitude portion of a lat/lon string.
     * @param lat_lon
     * @return
     * @throws org.hrva.hrtail.Reformat.InvalidRow 
     */
    String get_lat(String lat_lon) throws InvalidRow {
        try {
            String[] ll_item = lat_lon.split("/");
            String p1 = ll_item[0].substring(0, 2);
            String p2 = ll_item[0].substring(2, ll_item[0].length());
            return p1 + "." + p2;
        } catch (Exception ex) {
            throw new InvalidRow();
        }
    }

    /**
     * Get the longitude portion of a lat/lon string.
     * 
     * @param lat_lon
     * @return
     * @throws org.hrva.hrtail.Reformat.InvalidRow 
     */
    String get_lon(String lat_lon) throws InvalidRow {
        try {
            String[] ll_item = lat_lon.split("/");
            String p1 = ll_item[1].substring(0, 3);
            String p2 = ll_item[1].substring(3, ll_item[1].length());
            return p1 + "." + p2;
        } catch (Exception ex) {
            throw new InvalidRow();
        }
    }
    
    String get_valid(String word) throws InvalidRow {
        if( word.equals("[Valid]") ) return "V";
        return "I";
    }

    /**
     * Extract individual fields from an input line, creating
     * a mapping from column title to string value.
     * 
     * <p>Any invalid input throws an InvalidRow exception.</p>
     * 
     * <p>Examples</p>
     * <code>
     * 07:04:42 02/15 V.1.2233 H.0.0 MT_LOCATION Lat/Lon:370620935/-763413842
     * [Valid] Adher:-1 [Valid] Odom:2668 [Valid] DGPS:On FOM:2
     * </code>
     *
     * <code>
     * 07:04:42 02/15 V.1.2236 H.0.0 MT_TIMEPOINTCROSSING Time:07:04:36 Arrival
     * Rte:4 Dir:2 TP:329 Stop:45 Svc:1 Blk:221 Lat/Lon:370315618/-763461352
     * [Valid] Adher:2 [Valid] Odom:1924 [Valid] DGPS:On FOM:2
     * </code>
     *
     * @param line
     * @return Map<String,String> from column title to value.
     * @throws org.hrva.hrtail.Reformat.InvalidRow
     */
    public Map<String, String> extract_fields(String line) throws InvalidRow {
        Map<String, String> row = null;
        String[] words = line.split("\\s");
        if (words.length < 5) {
            throw new InvalidRow();
        } else if (words[4].equals("MT_LOCATION") && words.length == 13) {
            row = new TreeMap<String, String>();
            row.put("Time", get_time(words[0]));
            row.put("Date", get_date(words[1]));
            row.put("Vehicle", words[2]);
            row.put("H", words[3]);
            String lat_lon = label_value(words[5], "Lat/Lon");
            row.put("Lat", get_lat(lat_lon));
            row.put("Lon", get_lon(lat_lon));
            row.put("Location Valid/Invalid", get_valid(words[6]));
            row.put("Adherence", label_value(words[7], "Adher"));
            row.put("Adherence Valid/Invalid", get_valid(words[8]));
            row.put("Odom", label_value(words[9], "Odom"));
            row.put("Odom Valid/Invalid", get_valid(words[10]));
            row.put("DGPS", label_value(words[11], "DGPS"));
            row.put("FOM", label_value(words[12], "FOM"));

        } else if (words[4].equals("MT_TIMEPOINTCROSSING") && words.length == 21) {
            // Two flavors -- keep Arrival.  Drop Dwell.
            if (words[6].equals("Arrival")) {
                row = new TreeMap<String, String>();
                row.put("Time", get_time(words[0]));
                row.put("Date", get_date(words[1]));
                row.put("Vehicle", words[2]);
                row.put("H", words[3]);
                //Time:07:04:36 Arrival 
                row.put("Arrival", label_value(words[5], "Time"));
                //Rte:4 Dir:2 TP:329 Stop:45 Svc:1 Blk:221
                row.put("Route", label_value(words[7], "Rte"));
                row.put("Direction", label_value(words[8], "Dir"));
                row.put("TP", label_value(words[9], "TP"));
                row.put("Stop", label_value(words[10], "Stop"));
                row.put("Svc", label_value(words[11], "Svc"));
                row.put("Blk", label_value(words[12], "Blk"));
                String lat_lon = label_value(words[13], "Lat/Lon");
                row.put("Lat", get_lat(lat_lon));
                row.put("Lon", get_lon(lat_lon));
                row.put("Location Valid/Invalid", get_valid(words[14]));
                row.put("Adherence", label_value(words[15], "Adher"));
                row.put("Adherence Valid/Invalid", get_valid(words[16]));
                row.put("Odom", label_value(words[17], "Odom"));
                row.put("Odom Valid/Invalid", get_valid(words[18]));
                row.put("DGPS", label_value(words[19], "DGPS"));
                row.put("FOM", label_value(words[20], "FOM"));
            }
        } else {
            /*
             * Debugging:
             * 
            System.out.println("length " + words.length);
            System.out.println("line " + line);
            for (String w : words) {
                System.out.println("  '" + w + "'");
            }
            */
            throw new InvalidRow();
        }
        return row;
    }
}
