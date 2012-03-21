/*
 * The HRT Project.
 * Aavailable under a Creative Commons 2.0 License.
 */
package org.hrva.capture;

import java.io.*;
import java.text.MessageFormat;

/**
 * Timer Task to do Tail, Format and Push all in a single request.
 *
 * <p>This combines {@link org.hrva.capture.LogTail}, {@link org.hrva.capture.Reformat}
 * and {@link org.hrva.capture.CouchPush} into a single request.
 * </p>
 *
 * @author slott
 */
public class Tail_Format_Push extends CaptureWorker {

    String source_filename;
    String extract_filename;
    String csv_filename;
    LogTail tail;
    Reformat reformat;
    CouchPush push;

    Tail_Format_Push() {
        super();
    }
    
    /**
     * @param capture the capture to set
     */
    @Override
    public void setCapture(Capture capture) {
        this.capture = capture;
        tail = new LogTail(capture.global);
        reformat = new Reformat(capture.global);
        push = new CouchPush(capture.global);
    }


    @Override
    public void setCsv_filename(String csv_filename) {
        this.csv_filename = csv_filename;
    }

    @Override
    public void setExtract_filename(String extract_filename) {
        this.extract_filename = extract_filename;
    }

    @Override
    public void setSource_filename(String source_filename) {
        this.source_filename = source_filename;
    }

    /**
     * Run the timer task. 
     * 
     * <p> This performs the standard three-step capture.
     * </p> <ol> <li>LogTail</li> <li>Reformat</li> <li>CouchPush</li> </ol>
     */
    @Override
    public void run() {
        Object[] details = {source_filename, extract_filename, csv_filename};
        try {
            String created = tail.tail(source_filename, extract_filename);
            if (created == null) {
                return;
            }
            Reader extract = new FileReader(new File(created));
            File csv_file = new File(csv_filename);
            Writer wtr = new FileWriter(csv_file, false);
            reformat.include_header = true;
            try {
                capture.logger.info(MessageFormat.format("Reformatting {1} to {2}", details));
                reformat.reformat(extract, wtr);
            } catch( Exception ex ) {
                capture.logger.error("Reformat: " + ex.toString() );
            } finally {
                wtr.close();
            }
            capture.logger.debug("About to push " + csv_filename);
            push.open();
            try {
                push.push_feed(csv_file);
            }
            catch( Exception ex ) {
                capture.logger.error("CouchPush: " + ex.toString() );
                cancel();
            }
        } catch (Exception ex) {
            capture.logger.fatal("Worker Failed", ex);
            cancel();
            throw new Error(ex);
        }
    }
}
