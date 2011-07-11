/* *****************************************************************************
 *Copyright (c) 2011 Christian Ander & The Bioclipse Team with others.
 *All rights reserved. This program and the accompanying materials
 *are made available under the terms of the Eclipse Public License v1.0
 *which accompanies this distribution, and is available at
 *http://www.eclipse.org/legal/epl-v10.html
 *
 *Contact: http://www.bioclipse.net/
 *******************************************************************************/
package net.bioclipse.r.ui.views;
import net.bioclipse.r.business.Activator;
import net.bioclipse.r.business.IRBusinessManager;
import net.bioclipse.scripting.ui.views.ScriptingConsoleView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RConsoleView extends ScriptingConsoleView {

   final Logger logger = LoggerFactory.getLogger(RConsoleView.class);
   private IRBusinessManager r;

   public RConsoleView() {
	   logger.info("Starting R console UI");
	   getRBusinessManager();
   }

/*
 * Execute the R command - First check if r manager is available.
 */
    @Override
    protected String executeCommand( String command ) {
// 	TODO: fix a nicer welcome text
    	String returnVal = "Waiting for R Manager, please try again.";
    	echoCommand(command);
    	if (r != null)
    		returnVal = r.eval(command);
    	else getRBusinessManager();
      	printMessage(returnVal);
    	return returnVal;
    }

    private void getRBusinessManager() {
    	try {
    		r = Activator.getDefault().getJavaRBusinessManager();
    		printMessage(r.getStatus());
    		logger.debug(r.getStatus());
    	}
    	catch (IllegalStateException e) {
    		printMessage("Waiting for JavaRBusinessManager.");
    		logger.debug(e.getMessage());
    	}
    }
    
    protected void waitUntilCommandFinished() {
        // Don't know if there's a way to sensibly implement this method for R.
    }

    void echoCommand(final String command) {
        printMessage(NEWLINE + "> " + command + NEWLINE);
    }
}