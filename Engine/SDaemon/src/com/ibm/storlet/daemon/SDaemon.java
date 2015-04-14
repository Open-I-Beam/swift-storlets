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
 10-Jul-2014    evgenyl     Refactoring. Switching to SBus.
 ===========================================================================*/

package com.ibm.storlet.daemon;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.ibm.storlet.common.*;
import com.ibm.storlet.daemon.STaskFactory;
import com.ibm.storlet.sbus.*;

import java.util.concurrent.*;


/*----------------------------------------------------------------------------
 * SDaemon
 * 
 * This class acts as a language binding and management layer for 
 * user's Storlet logic implementation(~s?)  
 * */
public class SDaemon 
{

	private static ch.qos.logback.classic.Logger logger_;
	private static SBus sbus_;
	private static STaskFactory storletTaskFactory_;
	private static ExecutorService threadPool_;
	private static String strStorletName_;
	private static int nDefaultTimeoutToWaitBeforeShutdown_ = 3;
	/*------------------------------------------------------------------------
	 * initLog
	 * */
	private static boolean initLog( final String strClassName, 
	                                final String strLogLevel ) 
	{
	    Level newLevel = Level.toLevel( strLogLevel );
	    boolean bStatus = true;
		try {
			logger_ = (ch.qos.logback.classic.Logger)
			          LoggerFactory.getLogger("StorletDaemon_"+ strClassName);
			logger_.setLevel( newLevel );
			logger_.info("Logger Started");
		} 
		catch( Exception e )
		{
			System.err.println("got exception " + e); 
			bStatus = false;
		}
		return bStatus;
	}

    /*------------------------------------------------------------------------
     * loadStorlet
     * */
	private static IStorlet loadStorlet( final String strStorletClassName ) 
	{
		IStorlet storlet = null;
		try 
		{
			Class<?> c = Class.forName( strStorletClassName );
			storlet = (IStorlet)c.newInstance();
		} 
		catch( Exception e ) 
		{
			logger_.error( strStorletName_ + 
			               ": Failed to load storlet class " + 
			               strStorletClassName + 
			               "class path is " + 
			               System.getProperty( "java.class.path" ) );
			logger_.error( strStorletName_ + ": " + 
			               e.getStackTrace().toString() );
		}
		return storlet;
	}

	/*------------------------------------------------------------------------
	 * main
	 * 
	 * Entry point.
	 * args[0] - storlet class name
	 * args[1] - path to SBus
	 * args[2] - log level
	 * args[3] - thread pool size
	 * 
	 * Invocation from CLI example: 
	 * java -Djava.library.path=. ...
	 * 
	 * when packed in a .jar with the native .so use: 
	 * java 
	 * -Djava.library.path=. 
	 * -Djava.class.path=.:./storletdaemon.jar  
	 * com.ibm.storlet.daemon.StorletDaemon 
	 * <args>
	 * 
	 * where <args> can be: storlet.test.TestStorlet /tmp/aaa FINE 5
	 * 
	 * */
	public static void main( String[] args ) throws Exception 
	{
	    initialize( args );
	    mainLoop();
	    exit();
	}
	
	/*------------------------------------------------------------------------
	 * initialize
	 * 
	 * Initialize the resources
	 * */
	private static void initialize( String[] args ) throws Exception
	{
		strStorletName_ = args[0];
		String strSBusPath = args[1];
		String strLogLevel = args[2];
		int nPoolSize = Integer.parseInt(args[3]);
		
		if( initLog( strStorletName_, strLogLevel ) == false )
			return;

		IStorlet storlet = loadStorlet( strStorletName_ );
		if( storlet == null )
			return;
		
		storletTaskFactory_ = new STaskFactory( storlet, logger_ );
		logger_.trace("Instanciating SBus");
		sbus_ = new SBus();
		try 
		{
			logger_.trace("Initialising SBus");
			sbus_.create( strSBusPath );
		} 
		catch (IOException e) 
		{
			logger_.error( strStorletName_ + ": Failed to create SBus" );
			return;			
		}
        logger_.trace("Initialising thread pool with "+nPoolSize+" threads");		
		threadPool_ = Executors.newFixedThreadPool( nPoolSize );
	}
	
	/*------------------------------------------------------------------------
	 * mainLoop
	 * 
	 * The main loop - listen, receive, execute till the HALT command. 
	 * */
	private static void mainLoop() throws Exception
	{
		boolean doContinue = true;
		while( doContinue )
		{
			// Wait for incoming commands
			try 
			{
                logger_.trace( strStorletName_ + 
                        ": listening on SBus");
				sbus_.listen();
				logger_.trace( strStorletName_ + 
				               ": SBus listen() returned");
			} 
			catch( IOException e )
			{
				logger_.error( strStorletName_ + 
				               ": Failed to listen on SBus");
				doContinue = false;
				break;
			}
			
			logger_.trace(strStorletName_ + ": Calling receive");
			SBusDatagram dtg = null;
			try
			{
			    dtg = sbus_.receive();
			    logger_.trace( strStorletName_ + ": Receive returned" );
			}
			catch( IOException e )
			{
                logger_.error( strStorletName_ + 
                               ": Failed to receive data on SBus");
                doContinue = false;
                break;
			    
			}			
			// We have the request
			// Initialize a task according to command and execute it
			doContinue = processDatagram( dtg );
		}
	}
	
	/*------------------------------------------------------------------------
	 * processDatagram
	 * 
	 * Analyze the request datagram. Invoke the relevant storlet 
	 * or do some other job ( halt, description, or maybe something 
	 * else in the future ).
	 * */
	private static boolean processDatagram( SBusDatagram dtg )
	{
	    boolean bStatus = true;
	    SAbstractTask sTask = null;
		try 
		{
			logger_.trace(  strStorletName_ + 
			                ": Calling createStorletTask with " 
			                + dtg.toString());
			sTask = storletTaskFactory_.createStorletTask( dtg );
		} 
		catch( StorletException e )
		{
			logger_.trace( strStorletName_ + 
			               ": Failed to init task " + e.toString() );
			bStatus = false;
		}
			
		if( null == sTask ) 
		{
			logger_.error( strStorletName_ + 
			               ": Unknown command received Quitting" );
			bStatus = false;
		} 
		else if( sTask instanceof SHaltTask )
		{ 
			logger_.trace( strStorletName_ + ": Got Halt Command" );
			bStatus = false;
		} 
		else if( sTask instanceof SExecutionTask )
		{
			logger_.trace( strStorletName_ + ": Got Invoke command" );
			threadPool_.submit( (SExecutionTask) sTask );
		} 
		else if( sTask instanceof SDescriptorTask )
		{
			logger_.trace(strStorletName_ + ": Got Descriptor command" );
			((SDescriptorTask) sTask).run();
		}
		else if( sTask instanceof SPingTask )
		{
            logger_.trace(strStorletName_ + ": Got Ping command" );
		    bStatus = ((SPingTask) sTask).run();
		}
		return bStatus;
	}
	
	/*------------------------------------------------------------------------
	 * exit
	 * 
	 * Release the resources and quit
	 * */
	private static void exit()
	{
		logger_.info( strStorletName_ + 
		              ": Daemon for storlet " + 
		              strStorletName_ + 
		              " is going down...shutting down threadpool" );
		try 
		{
            threadPool_.awaitTermination(nDefaultTimeoutToWaitBeforeShutdown_,
                                         TimeUnit.SECONDS );
        } 
		catch( InterruptedException e ) 
		{
            e.printStackTrace();
        }
		threadPool_.shutdown();
		logger_.info( strStorletName_ + ": threadpool down" );
	}
}
/*============================== END OF FILE ===============================*/
