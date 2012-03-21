/*
 * The HRT Project.
 * Aavailable under a Creative Commons 2.0 License.
 */
package org.hrva.capture;

import java.text.MessageFormat;

/**
 * Timer Task to do Tail operations only.
 *
 * <p>This uses {@link org.hrva.capture.LogTail} to capture
 * The spysocket.log data.  
 * </p>
 * @author slott
 */
public class Tail_Only extends CaptureWorker {
    
    String source_filename;
    String extract_filename;
    String csv_filename;
    private final Capture capture;
    LogTail tail;

    Tail_Only(final Capture capture) {
        super();
        this.capture = capture;
        tail = new LogTail(capture.global);
    }

    /**
     * Run the timer task.
     * 
     * <p> This performs a LogTail operation.</p>
     */
    @Override
    public void run() {
        Object[] details = {source_filename, extract_filename, csv_filename};
        try {
            String created = tail.tail(source_filename, extract_filename);
            if (created == null) {
                return;
            }
            capture.logger.info(MessageFormat.format("Tail from {0} to {1}", details));
        } catch (Exception ex) {
            capture.logger.fatal("Worker Failed", ex);
            cancel();
            throw new Error(ex);
        }
    }

    @Override
    public void setCsv_filename(String csv_filename) {
        // Ignored.
    }

    /**
     * 
     * @param extract_filename
     */
    @Override
    public void setExtract_filename(String extract_filename) {
        this.extract_filename = extract_filename;
    }

    /**
     * 
     * @param source_filename
     */
    @Override
    public void setSource_filename(String source_filename) {
        this.source_filename = source_filename;
    }
    
}
