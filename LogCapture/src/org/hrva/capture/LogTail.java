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
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tail a rapidly growing log file and push an HRT Feeds to the CouchDB.
 *
 * <p>This is both a main program with a command-line interface, as well as
 * object that can be used to tail a log file. </p>
 *
 * <p>Typical use case</p>
 * <code><pre>
 *     LogTail lt = new LogTail();
 *     lt.tail( "/path/to/spysocket.log", "extract.txt" );
 *     System.out.print( "Created "extract.txt" );
 * </pre></code>
 *
 * <p>At the command line, it might look like this.</p> 
 * <code><pre>
 * java -cp LogTail/dist/LogTail.jar org.hrva.capture.LogTail -o extract.txt /path/to/spysocket.log
 * java -cp LogTail/dist/LogTail.jar org.hrva.capture.CouchPush -f extract.txt 
 * </pre></code>
 * 
 * @author slott
 */
public class LogTail {

    /** Properties for this application. */
    Properties global = new Properties();
    
    /** Logger. */
    final Log  logger = LogFactory.getLog(LogTail.class);

    /**
     * Command-line program to tail a log and then push file to the HRT couch
     * DB.
     * <p>All this does is read properties and invoke run_main</p>
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        Log log = LogFactory.getLog(LogTail.class);
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
        LogTail lt = new LogTail(config);
        try {
            lt.run_main(args);
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
    public LogTail( Properties global) {
        super();
        this.global= global;
    }

    /**
     * Tails the log and (optionally) pushes a feed file.
     *
     * <ol> <li>Get cached status info.</li> <li>Tail Log</li>
     * <li>Update cached status info.</li>
     * <li>(optionally) Send to
     * couchdb.</li>  </ol>
     *
     * @param args the command line arguments
     * @throws ParseException 
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void run_main(String[] args) throws ParseException, FileNotFoundException, IOException {
    
        Option verbose = new Option("v", "verbose", false, "Verbose logging");
        Option output = OptionBuilder.withArgName("outputfile").hasArgs(1).withDescription("Output File Name").create("o");

        Options options = new Options();
        options.addOption(verbose);
        options.addOption(output);

        CommandLineParser parser = new GnuParser();
        CommandLine line;
        List positional_args;
        String extract_filename="hrtrtf.txt";
        
        try {
            // parse the command line arguments
            line = parser.parse(options, args);
            positional_args = line.getArgList();
            if (positional_args.size() != 1) {
                throw new org.apache.commons.cli.ParseException("One file must be named");
            }
            if (line.hasOption("o")) {
                extract_filename= line.getOptionValue("o");
            }
        } catch (org.apache.commons.cli.ParseException exp) {
            // oops, something went wrong
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            throw exp;
        }

        for (Object source : positional_args) {
            String temp = tail((String)source, extract_filename);
        }
    }

    /**
     * Tail the given file if the size has changed and return a temp filename.
     *
     * <p>This returns a temp filename if the log being tailed has changed.
     * </p>
     * 
     * <p>The supplied target filename is -- actually -- a format string.
     * The available value, <<tt>{0}</tt> is the sequence number
     * that's saved in the history cache.</p>
     *
     * @param source The log filename to tail
     * @param target A temporary filename into which to save the tail piece.
     * @return temp filename, if the file size changed; otherwise null
     * @throws FileNotFoundException
     * @throws IOException
     */
    public String tail(String source, String target) throws FileNotFoundException, IOException {
        // The resulting file name (or null if the log did not grow).
        String temp_name = null;

        // Open our last-time-we-looked file.
        String cache_file_name = global.getProperty("logtail.tail_status_filename",
                "logtail.history");
        String limit_str = global.getProperty("logtail.file_size_limit",
                "1m"); // 1 * 1024 * 1024;
        int limit;
        if( limit_str.endsWith("m") || limit_str.endsWith("M") ) {
            limit= 1024*1024*Integer.parseInt(limit_str.substring(0,limit_str.length()-1));
        }
        else if( limit_str.endsWith("k") || limit_str.endsWith("K") ) {
            limit= 1024*Integer.parseInt(limit_str.substring(0,limit_str.length()-1));
        }
        else{
            limit = Integer.parseInt(limit_str);
        }

        Properties state = get_state(cache_file_name);

        // Find the previous size and sequence number
        String prev_size_str = state.getProperty("size." + source, "0");
        long prev_size = Long.parseLong(prev_size_str);
        String seq_str = state.getProperty("seq." + source, "0");
        long sequence = Long.parseLong(seq_str);

        Object[] details = {
            source, target, seq_str, prev_size_str 
        };
        logger.info(MessageFormat.format("Tailing {0} to {1}", details));
        logger.info(MessageFormat.format("Count {2}, Bytes {3}", details));
        sequence += 1;

        // Attempt to seek to the previous position
        long position = 0;
        File log_to_tail = new File(source);
        RandomAccessFile rdr = new RandomAccessFile(log_to_tail, "r");
        try {
            long current_size = rdr.length();
            if (current_size == prev_size) {
                // Same size.  Nothing more to do here.
                position = current_size;
            } else {
                // Changed size.  Either grew or was truncated.
                if (rdr.length() < prev_size) {
                    // Got truncated.  Read from beginning.
                    sequence = 0;
                    prev_size= 0;
                } else {
                    // Got bigger.  Read from where we left off.
                    rdr.seek(prev_size);
                }
                // Read to EOF or the limit.  
                // No reason to get greedy.
                int read_size;
                if (current_size - prev_size > limit) {
                    read_size = limit;
                    rdr.seek( current_size-limit );
                } else {
                    read_size = (int) (current_size - prev_size);
                }
                byte[] buffer = new byte[read_size];
                rdr.read(buffer);
                position = rdr.getFilePointer();

                // Write temp file
                Calendar cal= Calendar.getInstance();
                Object[] args = { sequence, cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE) };
                temp_name = MessageFormat.format(target, args);

                File extract = new File(temp_name);
                OutputStream wtr = new FileOutputStream(extract);
                wtr.write(buffer);
            }
        } finally {
            rdr.close();
        }

        // Update our private last-time-we-looked file.
        state.setProperty("size." + source, String.valueOf(position));
        state.setProperty("seq." + source, String.valueOf(sequence));
        save_state(cache_file_name, state);

        Object[] details2 = {
            source, target, seq_str, prev_size_str, 
            String.valueOf(sequence), String.valueOf(position)
        };
        logger.info(MessageFormat.format("Count {4}, Bytes {5}", details2));

        return temp_name;
    }

    /**
     * Get the saved file size state.
     *
     * @param name Properties file into which the file sizes were saved.
     * @return Properties object with saved file sizes.
     */
    public Properties get_state(String name) {
        Properties state = new Properties();
        File cache_file = new File(name);
        if (cache_file.exists()) {
            InputStream istr;
            try {
                istr = new FileInputStream(cache_file);
                state.load(istr);
            } catch (FileNotFoundException ex) {
                logger.warn("No history "+name, ex);
            } catch (java.io.IOException ex) {
                logger.warn("Problems with history "+name, ex);
            }
        }
        return state;
    }

    /**
     * Save the file size for next time we're executed.
     *
     * @param name Properties file into which the file sizes are saved.
     * @param state Properties object to persist.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void save_state(String name, Properties state) throws FileNotFoundException, IOException {
        OutputStream ostr = new FileOutputStream(name);
        state.store(ostr, "LogTail Cache");
    }
}
