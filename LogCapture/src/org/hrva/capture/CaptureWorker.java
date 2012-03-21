/*
 * The HRT Project.
 * Aavailable under a Creative Commons 2.0 License.
 */
package org.hrva.capture;

import java.util.TimerTask;

/**
 * Defines the TimerTask interface for Capture.
 * 
 * <p> This performs some or all of the steps
 * involved in 
 * capture. </p> <ol> <li>LogTail</li> <li>Reformat</li>
 * <li>CouchPush</li> </ol> 
 * 
 * @author slott
 */
public abstract class CaptureWorker extends TimerTask {

    Capture capture;

    /**
     * Sets the parent Capture instance. 
     * <p>This must be invoked first to set the relationship.</p>
     * @param capture 
     */
    abstract void setCapture(Capture capture);

    /**
     * Set the CSV name for reformat (if used).
     * @param csv_filename 
     */
    abstract void setCsv_filename(String csv_filename);

    /**
     * Set the extract file name for tail (if used).
     * @param extract_filename 
     */
    abstract void setExtract_filename(String extract_filename);

    /**
     * Set the source file name for tail (if used).
     * @param source_filename 
     */
    abstract void setSource_filename(String source_filename);

    
}
