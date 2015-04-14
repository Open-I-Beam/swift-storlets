/*----------------------------------------------------------------------------
 * Copyright IBM Corp. 2015, 2015 All Rights Reserved
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * Limitations under the License.
 * ---------------------------------------------------------------------------
*/

/*============================================================================
 DD-MMM-YYYY    eranr       Initial implementation.
 10-Jul-2014    evgenyl     Refactoring.
 ===========================================================================*/
package com.ibm.storlet.daemon;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;

/*----------------------------------------------------------------------------
 * SPingTask
 * 
 * Instantiate AbstractTask class. The primary usage intent is to response 
 * that this Storlet Daemon is running
 * */
public class SPingTask extends SAbstractTask 
{
    private OutputStream sOut_ = null;
    
    /*------------------------------------------------------------------------
     * CTOR
     * */
	public SPingTask(OutputStream sOut,
	                 Logger logger) 
	{
		super(logger);
		this.sOut_ = sOut;
	}
	
    /*------------------------------------------------------------------------
     * run
     * 
     * The actual response on "ping" command.
     * */
	public boolean run()
	{
	    boolean bStatus = true;
	    try 
	    {
            this.sOut_.write((new String("OK")).getBytes());
        } 
	    catch (IOException e) 
	    {
            e.printStackTrace();
            bStatus = false;
        }
        return bStatus;
	}
}
/*============================== END OF FILE ===============================*/
