/*
 * The HRT Project.
 * Aavailable under a Creative Commons 2.0 License.
 */
package org.hrva.capture;

import java.util.TimerTask;

/**
 * Defines the TimerTask interface for Capture.
 * @author slott
 */
public abstract class CaptureWorker extends TimerTask {

    /**
     * Run the timer task. <p> This performs some or all of the three steps
     * involved in 
     * capture. </p> <ol> <li>LogTail</li> <li>Reformat</li>
     * <li>CouchPush</li> </ol>
     */

    abstract void setCsv_filename(String csv_filename);

    abstract void setExtract_filename(String extract_filename);

    abstract void setSource_filename(String source_filename);
    
}
