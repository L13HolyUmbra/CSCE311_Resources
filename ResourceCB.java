package osp.Resources;
import java.util.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Utilities.*;
import osp.Memory.*;
/**
 * @OSPProject Resources
 * @author Dion de Jong
 * @email Ddejong@email.sc.edu
 * @version Final
 * @Date 4-8-15
 * @class ResourceCB
 * This class is responsible for actions related to manipulating resources, 
 * including acquiring RRBs for a resource and choosing to grant or suspend, 
 * releasing the resources attached to a thread if an RRB is granted, releasing
 * all resources attached to a thread if one is killed, and performing the 
 * bankers algorithm/deadlock detection program specifically geared towards
 * deadlock avoidance, and fixing it after the fact. 
*/


/**
    Class ResourceCB is the core of the resource management module.
    Students implement all the do_* methods.
    @OSPProject Resources
*/

public class ResourceCB extends IflResourceCB
{
	//create instance variables used throughout program: we will use a 
	//Hashtable to hold the RRBs from threads for a certain resource, a place holder RRB, 
	//and a array for work (that will act as available resources) throughout the deadlockDetection method. 
	private static Hashtable<ThreadCB,RRB> threadRRBTable = new Hashtable<ThreadCB,RRB>(); 
	private static RRB nullRRB = new RRB(null,null,0); 
	
   /**
    * @OSPProject Resources
    * @method Constructor
    * Creates a new ResourceCB instance with the given number of 
    * available instances. This constructor must have super(qty) 
    * as its first statement.
    * 
    * @param qty
    * @return N/A
    */
	
    public ResourceCB(int qty)
    {
        super(qty);
    }

    /**
       This method is called once, at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Resources
    */
    public static void init()
    {
    	//Initialize stuff... N/A
    }

   /**
    * @OSPProject Resources
    * @method do_acquire
    * Tries to acquire the given quantity of this resource.
    * Uses deadlock avoidance or detection depending on th
    * strategy in use, as determined by ResourceCB.getDeadlockMethod().     *
    * @param quantity
    * @return The RRB corresponding to the request.
    * If the request is invalid (quantity+allocated>total) then return null.
    */
    public RRB do_acquire(int quantity) 
    {
    	//create a temp thread for the try/catch
    	ThreadCB t = null; 
    	
    	//try catch block to avoid null pointer exception
    	try 
        {
    		//when checking if this is the real current thread, it is possible for this to be null
    		t = MMU.getPTBR().getTask().getCurrentThread();
        }
    	
    	catch (NullPointerException e)
    	{
    		//what if we catch?
    	}
    	
    	//return null if the request for a resource, place how much is allocated is more than total resource in system. 
    	//this corresponds to an invalid request
    	if ((quantity + (getAllocated(t))) > getTotal())
    	{
    		return null; 
    	}
    	
    	//next we will check if this thread holds an RRB for this resource. 
    	if (threadRRBTable.containsKey(t) == false)
    	{
    		//if it doesn't, we will place a null RRB for it. 
    		threadRRBTable.put(t, nullRRB); 
    	}
    	
    	//create the true RRB that will be added
    	RRB newRRB = new RRB(t, this, quantity); 
		
    	//System.out.println("Acquired RRB: " + newRRB.getID() + ". Request of: " + quantity + " for Resource " + this.getID() + "\n");
    	//if the RRB has a request that we have enough available for, we will grant it immediately. 
    	if (quantity <= getAvailable())
    	{
    		//Jump to student create grant method in RRB.java
    		newRRB.grant(); 	
    		//threadRRBTable.put(t, nullRRB);
    	}
    	
    	else
    	{
    		//otherwise, we will assume that we need more resources, 
    		if (t.getStatus() != ThreadWaiting) 
    		{
    			//and suspend the RRB until more resources become available
    			newRRB.setStatus(Suspended);
    			t.suspend(newRRB);
    		}		
    	
    		if(!threadRRBTable.containsValue(newRRB))
		    {
				threadRRBTable.put(t, newRRB);	
	    	} 
    	}
    	
    	//return the newly created request.
    	return newRRB; 
    }

    
    /**
    * @OSPProject Resources
    * @method DeadLockDetection
    * Performs deadlock detection. This method will check if a deadlock occurs,
    * and it will check if there is a way to fix the deadlock and the attempt to
    * fix it. 
    * 
    * @param N/A
    * @return The vector of ThreadCB objects found to be in a deadlock.
    */
    public static Vector do_deadlockDetection()
    {
    	//create a vector of theads that are causing deadlock when we detect. 
    	Vector deadlockedThreads = BankersAlgorithm();
    	//if this vector becomes empty, there is no more deadlock
    	if (deadlockedThreads == null)
    	{
    		return null;
    	}
    	//Once we know of a dead lock, we are responsible for fixing it
    	Fix_deadLock(deadlockedThreads);
    	return deadlockedThreads; 
    }
    
    /**
    * @OSPProject Resources
    * @method Bankers Algorithm
    * This method specifically checks if there are any RRBs responsible 
    * for creating a deadlock and will return this.
    * 
    * @param N/A
    * @return A vector of ThreadCB objects found to be in a deadlock.
    */
    public static Vector<ThreadCB> BankersAlgorithm()
    {
    	//Create temporary int array for new available resources
    	int[] Work = new int[ResourceTable.getSize()];

    	//first, we must add the available numbers of resources for each resource to the work array. 
    	for (int i = 0; i < Work.length; i++)
    	{
    		Work[i] = ResourceTable.getResourceCB(i).getAvailable();
    	}
    	
    	//Then we will create an enumeration through the RRB hashtable in order to loop through each threads RRB 
    	//for this resource. 
    	Enumeration keys = threadRRBTable.keys(); 
    	
    	Hashtable<ThreadCB, Boolean> DLHash = new Hashtable<ThreadCB, Boolean>();
    	//We want to create a Hashtable using booleans to keep track of the resources we currently believe to be in deadlock. 
    	while (keys.hasMoreElements())
    	{
    		ThreadCB CurrentThread = (ThreadCB)keys.nextElement(); 
    		//initialize them all as false, assuming none are in deadlock
    		DLHash.put(CurrentThread, false);
    	}
    	
    	//we will now loop through the RRB hash table again
    	keys = threadRRBTable.keys(); 
    	while (keys.hasMoreElements()) //may need check for only threads allocated something
    	{    		
    		ThreadCB CurrentThread = (ThreadCB)keys.nextElement(); 
    		//For each element in the enumeration, check every resource on the resourcetable
    		for (int i = 0; i < ResourceTable.getSize(); i++)
    		{
    			ResourceCB CurrentResource = ResourceTable.getResourceCB(i);
    			if (CurrentResource.getAllocated(CurrentThread) != 0)
   				{
   					//and consider this thread a potential deadlock threat and set them to true to test
   					DLHash.put(CurrentThread, true);
   				}
    		}		
    	}
    	
    	//now we will create a while loop that will run until no new resources can be granted based on looking at Work[]
    	boolean Repeat = true; 
    	while (Repeat == true)
    	{	 
    		Repeat = false; 
    		//we will enumerate through the RRB hashtable again.
    		keys = threadRRBTable.keys(); 
    		while (keys.hasMoreElements())
    		{
    			ThreadCB CurrentThread = (ThreadCB)keys.nextElement(); 
    			//if we find that some of the current resource has been allocated to the currrent thread
    			if ((Boolean)DLHash.get(CurrentThread) == true)
    			{
    				RRB CurrentRRB = threadRRBTable.get(CurrentThread);
    				int CurrentRequest = CurrentRRB.getQuantity(); 
    				ResourceCB CurrentResource = CurrentRRB.getResource();
    				
    				//we will check if the current thread has a request that CAN be granted.
    				if (CurrentRequest == 0 || CurrentRequest <= Work[CurrentResource.getID()])
    				{    					
    					//Loop through the resource table again
    					for (int i = 0; i < ResourceTable.getSize(); i++)
    		    		{
    						//Add the allocated resource on the thread that is grantable to the work array
    		    			ResourceCB ResourcefromTable = ResourceTable.getResourceCB(i);
    		    			Work[i] += ResourcefromTable.getAllocated(CurrentThread); 
    		    		}
    					//we can then assume that this thread is not responsible for the current deadlock 
    					DLHash.put(CurrentThread, false);
    					//if any resource was added to work[], we are forced to have to loop through again and check other threads that could be granted
    					Repeat = true; 
    				}
    			}
    		}
    	}
    	
    	//Now we will enumerate the boolean hash, 
    	Enumeration createVector = DLHash.keys(); 
    	Vector<ThreadCB> deadLockedThreads = new Vector<ThreadCB>(); 
    	
    	while (createVector.hasMoreElements())
    	{
    		ThreadCB Cur =   (ThreadCB)createVector.nextElement();
    		boolean DL = DLHash.get(Cur); 
    		if (DL == true)
    		{
    			//and add any threads believed to be responsible for deadlock, based on the fact that they could not be granted their requests
    			//in the previous section of code. This are the threads likely responsible for the deadlock. 
    			deadLockedThreads.add(Cur);
    		}
    	}
    	Work = null;
    	//if there are no threads in the Hashtable that show true
    	if (deadLockedThreads.isEmpty())
    	{
    		//there is no deadlock
    		System.out.println("No Deadlock");
    		return null; 
    	}
    	
    	//we will return this list as the threads responsible. 
    	if (!deadLockedThreads.isEmpty())
    	{
    		//If there are any threads, we have a deadlock
    		System.out.println("DEADLOCK!");
    	}
    	return deadLockedThreads; 
    }
    
    /**
    * @OSPProject Resources
    * @method Fix Deadlock
    * This method attempts to fix the deadlock by killing a thread and checking
    * if the killed thread allows the deadlock to be fixed
    * 
    * @param the vector of potential deadlocked threads
    * @return the vector if threads still in deadlock (will always be null)
    */
    public static Vector<ThreadCB> Fix_deadLock(Vector<ThreadCB> ThreadsList)
    {
    	Vector<ThreadCB> DLThreads = ThreadsList; 
    	//loop through the list of potential deadlock threads
    	//continue looping until bankers returns null
    	while (DLThreads != null)
    	{ 
    		ThreadCB CurrentT = (ThreadCB)DLThreads.get(0);
    		//double check if there is still a deadlock
    		DLThreads = BankersAlgorithm();
    		if (DLThreads == null)
    		{
    			System.out.println("Fix break");
    			//if there is no more deadlock, we are finished
    			break; 
    		}
			
    		//if there is, kill the thread in the vector,
    		CurrentT.kill();   		
    		
        	//remove it from the suspected list, since it is gone. 
    		DLThreads.remove(CurrentT);
    	}
    	

		
    	//check which RRBs can be granted after a thread has been killed
    	RRB GrantRRB = CheckGrantable();
    	while (GrantRRB != null)
    	{
    		System.out.println(threadRRBTable.get(GrantRRB));
    		if (GrantRRB.getThread().getStatus() != ThreadKill)
    		{
    			//if the RRBs thread isn't being killed it can be granted
    			GrantRRB.setStatus(Granted); 
    			GrantRRB.grant();
    		}
    		//regardless, the RRBTable gets a null value as the RRB
    		threadRRBTable.put(GrantRRB.getThread(), nullRRB);
        	GrantRRB = CheckGrantable(); 
    	}
    	return DLThreads;
    }   
    
    /**
    * @OSPProject Resources
    * @method do_giveupResources
    * When a thread was killed, this is called to release all
    * the resources owned by that thread. This method will then 
    * attempt to grant any grantable RRBs 
    * 
    * @param thread
    * @return N/A
    */
    public static void do_giveupResources(ThreadCB thread)
    {   	
    	//Loop through the Resource Table
    	for (int i = 0; i < ResourceTable.getSize(); i++)
    	{ 
        	ResourceCB CurrentResource = ResourceTable.getResourceCB(i);
        	if (CurrentResource.getAllocated(thread) != 0)
        	{
        	//Set the Available to what the thread to be killed is holding, and set the allocated for each resource on the thread to 0
        	CurrentResource.setAvailable(CurrentResource.getAvailable() + CurrentResource.getAllocated(thread));     
           	CurrentResource.setAllocated(thread, 0); 
        	}
    	}
    	//remove this thread from the RRBTable to avoid dealing with it.
    	threadRRBTable.remove(thread);
    	
    	//Check if any RRBs can be granted
    	RRB GrantRRB = CheckGrantable();
    	while (GrantRRB != null)
    	{
    		if (GrantRRB.getThread().getStatus() != ThreadKill)
    		{
    			if (GrantRRB.getThread() != thread) 
    			{
    				//if the RRB's thread isn't being killed and isn't the current thread having stuff released, Grant it
    				GrantRRB.grant();    			
    			}
    		}
    		//Regardless, a grantable RRB will be set to null on the RRBTable.
    		threadRRBTable.put(GrantRRB.getThread(), nullRRB); 
    		GrantRRB = CheckGrantable();
    	}
    }

   /**
    * @OSPProject Resources
    * @method CheckGrantable
    * Release a previously acquired resource. This method will
    * be called by OSP to release a set amount of a certain resource.
    * It will attempt to grant any possible RRBs as a result of the resources released. 
    * 
    * @param quantity
    * @return N/A
    */
    public void do_release(int quantity)
    {
    	//create a temp thread for the try/catch
    	ThreadCB t = null; 
    	
    	//try catch block to avoid null pointer exception
    	try 
        {
    		//when checking if this is the real current thread, it is possible for this to be null
    		t = MMU.getPTBR().getTask().getCurrentThread();
        }
    	catch (NullPointerException e)
    	{
    		//what if we catch?
    	}
    	
    	//
    	int CurrentAlloc = getAllocated(t); 
    	
    	//if the desired release amount is higher than the allocation, set it equal
    	if (quantity > CurrentAlloc)
    	{
    		quantity = CurrentAlloc; 
    	}
    	
    	//set the RRB thread's allocation of the resource and the Resource's available to the new amount
    	setAllocated(t, CurrentAlloc - quantity); 
    	setAvailable(getAvailable() + quantity);
    	
    	//Call CheckGrantable to find a RRB that can now be grantd
    	RRB GrantRRB = CheckGrantable();
    	//continue as long as there as an RRB that is returned
    	while (GrantRRB != null)
    	{
        	//System.out.println("Grant " + GrantRRB.getID() + " is grantable"); 
    		if (GrantRRB.getThread().getStatus() != ThreadKill)
    		{
    			//Grant the resource if it is not null and it's thread isn't being killed
    			GrantRRB.setStatus(Granted);
    			GrantRRB.grant();
    		}
    		//if a request can be granted, and is not granted, we still want all cases to be added back 
    		//to the ThreadRRBTable as a null. 
    		threadRRBTable.put(GrantRRB.getThread(), nullRRB);
    		//Try to find another grantable RRB
        	GrantRRB = CheckGrantable(); 
    	}
    }

    /**
    * @OSPProject Resources
    * @method CheckGrantable
    * Iterate through the threadRRBTable and attempts to find any
    * RRBs that are able to be granted, given the Current Available 
    * Resources within the system. 
    *
    * @param N/A
    * @return The first RRB that can be granted
    * If there are no grantable requests, return null. 
    */
    public static RRB CheckGrantable()  
    {
    	//Create a collection
    	Collection c = threadRRBTable.values(); 
    	
    	//create a collection and iterator to iterate through the RRB hash table. 
    	Iterator iter = c.iterator(); 
    	RRB Check = nullRRB; 
    	
    	//Iterate through the collection
    	while(iter.hasNext())
    	{
    		//save variables to work with
    		Check = (RRB)iter.next();
    		ThreadCB TempThread = Check.getThread();	 
    		
    		//if the thread is null, skip ahead to the next item in the collection
    		if(Check.getThread() == null)
    		{
    			continue;
    		}
    		
    		//If the RRB is not null
    		if (Check != null)
    		{
    			//Grant condition
    			boolean Grant = false;
    			//Loop through resource table
    			for (int i = 0; i < ResourceTable.getSize(); i++)
    			{
    				ResourceCB CurrentResource = ResourceTable.getResourceCB(i);
    				//The resource of the RRB must match the one accessed in the Table.
    				if (CurrentResource == Check.getResource())
    				{
    					//System.out.println("Current Resource in Grantable loop Available : " + CurrentResource.getAvailable() + "\n");
    					//Set the Grant Condition to true if there is enough of that resource Available to grant to the RRB
    					if (Check.getQuantity() <= Check.getResource().getAvailable()) 
      					{
       						Grant = true; 
       					}  	
    				}			
   				}
    			//return this RRB if the condition changed to true while looping through the Resource Table
    			if (Grant == true)
    			{
    				return Check;
    			}
    		}
    	}
    	//if looping through the whole ThreadRRBTable did not grant a true, there are no grantable RRBs, return null.
    	return null; 
    }
    
    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
	
	@OSPProject Resources
    */
    public static void atError()
    {
        // your code goes here
    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
	@OSPProject Resources
    */
    public static void atWarning()
    {
        // your code goes here
    }

    /*
       Feel free to add methods/fields to improve the readability of your code
    */
}

/*
      Feel free to add local classes to improve the readability of your code
*/
