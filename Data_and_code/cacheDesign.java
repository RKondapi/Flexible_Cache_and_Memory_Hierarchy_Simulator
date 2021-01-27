import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class cacheDesign {
	public int blockSize;
	public String traceFile;
	public String[] address;
	public List<String[]> addressAfterSplit= new ArrayList<String[]>();
	public int replacementPolicy;
	public String[][] cacheL1;
	public String[][] cacheL2;
	public boolean[][] dirtyBitsL2;
	public List<String[]> addressAfterSplitL2 = new ArrayList<String[]>();
	public int[][] countQueueL2;
	public int[][] countLRUL2;
	public String[][] countOptimalL2;
	public String[][] countOptimalAddL2;
	public int inclusionProperty;
	public boolean[][] dirtyBits;
	public int L2Assoc;
	public int L2Size;
	public int L1Assoc;
	public int L1Size;
	public int[][] countQueue;
	public int[][] countLRU;
	public String[][] countOptimal;
	public String[][] countOptimalAdd;
	public int L1writeMiss=0;
	public int L1readMiss=0;
	public int L1reads=0;
	public int L1writes=0;
	public int L1writeBacks=0;
	public int L2writeMiss=0;
	public int L2readMiss=0;
	public int L2reads=0;
	public int L2writes=0;
	public int L2writeBacks=0;
	
	public int cacheBlockCountL1;
	public int setCountL1;
	public int indexBitsCountL1;
	public int blockOffsetBitsCountL1;
	public int tagBitsCountL1;
	
	public int conflictMissRate=0;
	public int cacheBlockCountL2;
	public int setCountL2;
	public int indexBitsCountL2;
	public int blockOffsetBitsCountL2;
	public int tagBitsCountL2;
	public int MemoryTraffic=0;
	
	//Initializing all the crucial variables necessary for calculation
	public cacheDesign(String[] args) 
	{
		//Block size common for both L1 and L2
		blockSize=Integer.valueOf(args[0]);
		//Variables using arguments from the console
		L1Size=Integer.valueOf(args[1]);
		L1Assoc=Integer.valueOf(args[2]);
		L2Size=Integer.valueOf(args[3]);
		L2Assoc=Integer.valueOf(args[4]);
		replacementPolicy=Integer.valueOf(args[5]);
		inclusionProperty=Integer.valueOf(args[6]);
		traceFile=String.valueOf(args[7]);
		//Checking of the replacement policy and inclusion property inputs are valid
		checkValidation();
		//Calculating L1 details (tag, index, offset, etc)
		calculateDetails(1,L1Size,L1Assoc);
		//Initializing arrays required for storing counters for replacement policies and mainly the cache for L1
		countQueue=new int[setCountL1][L1Assoc];
		dirtyBits=new boolean[setCountL1][L1Assoc];
		countOptimalAdd=new String[setCountL1][L1Assoc];
		countOptimal=new String[setCountL1][L1Assoc];
		countLRU=new int[setCountL1][L1Assoc];
		cacheL1=new String[setCountL1][L1Assoc];
		//Calculating L2 details (tag, index, offset, etc) only if the associativity of L2 is non-zero
		//Also initializing arrays required for storing counters for replacement policies and mainly the cache for L1
		if(L2Assoc!=0)
		{
			calculateDetails(2,L2Size,L2Assoc);
			cacheL2=new String[setCountL2][L2Assoc];
			countQueueL2=new int[setCountL2][L2Assoc];
			dirtyBitsL2=new boolean[setCountL2][L2Assoc];
			countLRUL2=new int[setCountL2][L2Assoc];
			countOptimalL2=new String[setCountL2][L2Assoc];
			countOptimalAddL2=new String[setCountL2][L2Assoc];
		}
		//This is where the logic starts, we get instructions and perform necessary functions to get the final output
		getInstructions(tagBitsCountL1, indexBitsCountL1, blockOffsetBitsCountL1, L1Assoc, setCountL1);
		//Function to display results on the console
		displayResults();
		
	}
	
	//Calculating details (tag, index, offset) for L1 and L2
	void calculateDetails(int cacheLevel, int size, int associativityNo)
	{
		if(cacheLevel==1)
		{
			cacheBlockCountL1=size/blockSize;
			setCountL1=cacheBlockCountL1/associativityNo;
			indexBitsCountL1=Integer.valueOf((int) (Math.log(setCountL1)/Math.log(2)));
			blockOffsetBitsCountL1=Integer.valueOf((int) (Math.log(blockSize)/Math.log(2)));
			tagBitsCountL1=32-indexBitsCountL1-blockOffsetBitsCountL1;
		}
		else if(cacheLevel==2)
		{
			cacheBlockCountL2=size/blockSize;
			setCountL2=cacheBlockCountL2/associativityNo;
			indexBitsCountL2=Integer.valueOf((int) (Math.log(setCountL2)/Math.log(2)));
			blockOffsetBitsCountL2=Integer.valueOf((int) (Math.log(blockSize)/Math.log(2)));
			tagBitsCountL2=32-indexBitsCountL2-blockOffsetBitsCountL2;
		}
		
	}
	
	//Logic begin
	void getInstructions(int tagBits,int indexBits,int offsetBits,int associativity, int sets) {
		List<String> instructionsList = new ArrayList<String>();

		int totalBits=tagBits+indexBits+offsetBits;
		int lineNo=0;
		File traceFileRead = new File("./"+traceFile);
		//To check if the file is valid performing exception handling. If it is valid get all the instructions per each line
		try 
		{
			List<String> instructions=Files.readAllLines(Paths.get("./"+traceFile));
		    for(String eachInstruction:instructions)
		    {
		    	lineNo++;
		    	instructionsList.add(eachInstruction);
		    }
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		int eachInst=0;
		//Getting only r or w from the instructions in the trace file
		String[] readOrWrite = new String[instructionsList.size()];
		//Getting only addresses from the instructions in the trace file
		address = new String[instructionsList.size()];
		for(String perInstruction:instructionsList) 
		{
			readOrWrite[eachInst]=perInstruction.split(" ")[0];
			address[eachInst]=perInstruction.split(" ")[1];
			eachInst++;
		}
		//Separating tag, index, offset bits (for L1) and storing them in a list of string arrays (addressAfterSplit)
		for(String perAddress:address) 
		{
			String binary=hexToBinary(perAddress, totalBits);
			String tagsHexConv=binaryToHex(binary.substring(0, tagBits));      
		    String indexsBinConv;
		    if(binary.substring(tagBits, tagBits+indexBits).equals(""))
		    {
		    	indexsBinConv="0";
		    }
		    else
		    {
		    	indexsBinConv=binaryToDec(binary.substring(tagBits, tagBits+indexBits));
		    }
		    String offsetsHexConv=binaryToHex(binary.substring(tagBits+indexBits, tagBits+indexBits+offsetBits));
		    addressAfterSplit.add(new String[] {tagsHexConv,indexsBinConv,offsetsHexConv});
		}
		//Separating tag, index, offset bits (for L2) and storing them in a list of string arrays (addressAfterSplitL2)
		if(L2Assoc!=0)
		{
			for(String perAddress:address) 
			{
				String binary=hexToBinary(perAddress, totalBits);
				String tagsHexConv=binaryToHex(binary.substring(0, tagBitsCountL2));      
			    String indexsBinConv;
			    if(binary.substring(tagBits, tagBits+indexBits).equals(""))
			    {
			    	indexsBinConv="0";
			    }
			    else
			    {
			    	indexsBinConv=binaryToDec(binary.substring(tagBits, tagBits+indexBits));
			    }
			    String offsetsHexConv=binaryToHex(binary.substring(tagBitsCountL2+indexBitsCountL2, tagBitsCountL2+indexBitsCountL2+offsetBits));
			    addressAfterSplitL2.add(new String[] {tagsHexConv,indexsBinConv,offsetsHexConv});
			}
		}
	  
	  String op;
	  int indexBit;
	  int rep=0;
	  int ind;
	  //Main logic: inserting into cache (L1)
	  first:
	  for(int i=0;i<addressAfterSplit.size();i++)
	  {
		  //Get the index bit of first instruction
		  indexBit=Integer.valueOf(addressAfterSplit.get(i)[1]);
		  //Get the operation (r or w) for the first instruction
		  op=readOrWrite[i];
		  if(op.equalsIgnoreCase("r")) 
		  {
			  //If it is r then increment reads in L1
			  L1reads++;
			  for(int j=0;j<cacheL1.length;j++)
			  {
				  //Iterate through the rows in the cache till the index bit is found
				  if(j!=indexBit)
					  continue;
				  for(int k=0;k<cacheL1[j].length;k++)
				  {
					  //Get the associativity, check if it null
					  if(Objects.isNull(cacheL1[j][k]) || cacheL1[j][k]==null)
					  {
						  //Compulsory miss if null (increment L1 read miss)
						  L1readMiss++;
						  //Also perform the read operation on L2 if the L2 associativity is non-zero
						  if (L2Assoc!=0)
			        	  {
			        	    L2read(addressAfterSplit.get(i)[0],j,i);
			        	  }
						  //Insert the tag address of the instruction to that block (null block)
						  cacheL1[j][k]=addressAfterSplit.get(i)[0];
						  //Creating a duplicate cache for optimal policy
						  countOptimal[j][k]=addressAfterSplit.get(i)[0];
						  countOptimalAdd[j][k]=address[i];
						  //Incrementing queue block in array for FIFO policy
						  countQueue[j][k]=(Arrays.stream(countQueue[j]).max().getAsInt())+1;
						  //Incrementing LRU block in the array for LRU policy
						  countLRU[j][k]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
						  //next instruction
						  continue first;
					  }
					  else if(cacheL1[j][k].equalsIgnoreCase(addressAfterSplit.get(i)[0]))
					  {
						  //if hit, then increment LRU block in the array for LRU policy
						  countLRU[j][k]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
						  //next instruction
						  continue first;
					  }
				  }
				  for(int k=0;k<cacheL1[j].length;k++)
				  {
					  //check the number of misses
					  if(!cacheL1[j][k].equalsIgnoreCase(addressAfterSplit.get(i)[0]))
					  {
						  rep++;
					  }
				  }
				  if(rep==L1Assoc)
				  {
					  //if the number of misses=associativity then its time to replace a block
					  //conflict miss rate also increases
					  conflictMissRate++;
					  rep=0;
					  //increment read miss
					  L1readMiss++;
					  //Go to the specified replacement policy
				      switch(replacementPolicy)
				      {
				        case 0:
				        	//Get the index to be replaced (which is returned by LRU function)
				        	ind=leastRecentlyUsed(countLRU[j],cacheL1[j],j,addressAfterSplit.get(i)[0],addressAfterSplit.get(i)[1]);
				        	//If the replaced index block is a dirty bit then we should write-back
				        	if(dirtyBits[j][ind])
							{
				        		//Increment L1 write-back
				        		L1writeBacks++;
				        		//Write the replaced block to L2 if the L2 associativity is mentioned
				        		if (L2Assoc!=0)
				        		{
				        			L2write(cacheL1[j][ind],j,i);
				        		}
							}
				        	//Also read the original block (current instruction) to L2 (if the L2 associativity is mentioned)
				        	if (L2Assoc!=0)
				        	{
				        	   L2read(addressAfterSplit.get(i)[0],j,i);
				        	}
				        	//Remove the dirty bits in the block and update the cache
				        	dirtyBits[j][ind]=false;
				        	cacheL1[j][ind]=addressAfterSplit.get(i)[0];
				        	countOptimal[j][ind]=addressAfterSplit.get(i)[0];
				        	countOptimalAdd[j][ind]=address[i];
				        	//Update the LRU counter
							countLRU[j][ind]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
				        	break;
						case 1:
							//Get the index to be replaced (which is returned by queue function)
							ind=queue(countQueue[j],cacheL1[j],j,addressAfterSplit.get(i)[0],addressAfterSplit.get(i)[1]);
							//If the replaced index block is a dirty bit then we should write-back
							if(dirtyBits[j][ind])
							{
								//Increment L1 write-back
								L1writeBacks++;
								//Write the replaced block to L2 if the L2 associativity is mentioned
								if (L2Assoc!=0)
								{
									L2write(cacheL1[j][ind],j,i);
								}
							}
							//Also read the original block (current instruction) to L2 (if the L2 associativity is mentioned)
							if (L2Assoc!=0)
				        	{
								L2read(addressAfterSplit.get(i)[0],j,i);
				        	}
							//Remove the dirty bits in the block and update the cache
							dirtyBits[j][ind]=false;
							cacheL1[j][ind]=addressAfterSplit.get(i)[0];
							countOptimal[j][ind]=addressAfterSplit.get(i)[0];
							countOptimalAdd[j][ind]=address[i];
							//Update the queue (FIFO) counter
							countQueue[j][ind]=(Arrays.stream(countQueue[j]).max().getAsInt())+1;
							break;
						case 2:
							//Get the index to be replaced (which is returned by optimal function)
							ind=optimalPolicy("L1",countOptimal[j],i,countOptimalAdd[j],j);
							//If the replaced index block is a dirty bit then we should write-back
							if(dirtyBits[j][ind])
							{
								//Increment L1 write-back
								L1writeBacks++;
								//Write the replaced block to L2 if the L2 associativity is mentioned
								if (L2Assoc!=0)
								{
									L2write(cacheL1[j][ind],j,i);
								}
							}
							//Also read the original block (current instruction) to L2 (if the L2 associativity is mentioned)
							if (L2Assoc!=0)
				        	{
								L2read(addressAfterSplit.get(i)[0],j,i);
				        	}
							//Remove the dirty bits in the block and update the cache
							dirtyBits[j][ind]=false;
							cacheL1[j][ind]=addressAfterSplit.get(i)[0];
							//Update the optimal array
							countOptimal[j][ind]=addressAfterSplit.get(i)[0];
							countOptimalAdd[j][ind]=addressAfterSplit.get(i)[0];
							break;
				      }
				  }
			  }
		  }
		  
		  //Same logic of read operation applied to write operation as well
		  if(op.equalsIgnoreCase("w")) 
		  {
			//If it is w then increment writes in L1
			  L1writes++;
			  for(int j=0;j<cacheL1.length;j++)
			  {
				//Iterate through the rows in the cache till the index bit is found
				  if(j!=indexBit)
				  {
					  continue;
				  }
				//Get the associativity, check if it null
				  for(int k=0;k<cacheL1[j].length;k++)
				  {
					  if(Objects.isNull(cacheL1[j][k]) || cacheL1[j][k]==null)
					  {
						  //Compulsory miss if null (increment L1 write miss)
						  L1writeMiss++;
						  //Also perform the read operation on L2 if the L2 associativity is non-zero
						  if (L2Assoc!=0)
			        	  {
			        	    L2read(addressAfterSplit.get(i)[0],j,i);
			        	  }
						  //Insert the tag address of the instruction to that block (null block)
						  cacheL1[j][k]=addressAfterSplit.get(i)[0];
						  //Creating a duplicate cache for optimal policy
						  countOptimal[j][k]=addressAfterSplit.get(i)[0];
						  countOptimalAdd[j][k]=address[i];
						  //Incrementing queue block in array for FIFO policy
						  countQueue[j][k]=(Arrays.stream(countQueue[j]).max().getAsInt())+1;
						  //Incrementing LRU block in array for LRU policy
						  countLRU[j][k]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
						  //Set dirty bit
						  dirtyBits[j][k]=true;
						  //next instruction
						  continue first;
					  }
					  else if(cacheL1[j][k].equalsIgnoreCase(addressAfterSplit.get(i)[0]))
					  {
						  //if hit, then increment LRU block in the array for LRU policy
						  countLRU[j][k]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
						  //Set dirty bit
						  dirtyBits[j][k]=true;
						  //next instruction
						  continue first;
					  }
				  }
				  for(int k=0;k<cacheL1[j].length;k++)
				  {
					  if(!cacheL1[j][k].equalsIgnoreCase(addressAfterSplit.get(i)[0]))
					  {
						//check the number of misses
						  rep++;
					  }
					  if(cacheL1[j][k].equalsIgnoreCase(addressAfterSplit.get(i)[0]))
					  {
						  countLRU[j][k]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
						  dirtyBits[j][k]=true;
						  continue first;
					  }
				  }
				  if(rep==L1Assoc)
				  {
					  //if the number of misses=associativity then its time to replace a block
					  //conflict miss rate also increases
					  rep=0;
					  conflictMissRate++;
					  //increment write miss
					  L1writeMiss++;
					  //Go to the specified replacement policy
				      switch(replacementPolicy)
				      {
				        case 0:
				        	//Get the index to be replaced (which is returned by LRU function)
				        	ind=leastRecentlyUsed(countLRU[j],cacheL1[j],j,addressAfterSplit.get(i)[0],addressAfterSplit.get(i)[1]);
				        	//If the replaced index block is a dirty bit then we should write-back
				        	if(dirtyBits[j][ind])
							{
				        		//Increment L1 write-back
				        		L1writeBacks++;
				        		//Write the replaced block to L2 if the L2 associativity is mentioned
				        		if (L2Assoc!=0)
				        		{
				        			L2write(cacheL1[j][ind],j,i);
				        		}
							}
				        	//Also read the original block (current instruction) to L2 (if the L2 associativity is mentioned)
				        	if (L2Assoc!=0)
				        	{
				        	   L2read(addressAfterSplit.get(i)[0],j,i);
				        	}
				        	//Set the dirty bits in the block and update the cache
				        	cacheL1[j][ind]=addressAfterSplit.get(i)[0];
				        	countOptimal[j][ind]=addressAfterSplit.get(i)[0];
				        	countOptimalAdd[j][ind]=addressAfterSplit.get(i)[0];
				        	//Update the LRU counter
							countLRU[j][ind]=(Arrays.stream(countLRU[j]).max().getAsInt())+1;
				        	dirtyBits[j][ind]=true;
				        	break;
						case 1:
							//Get the index to be replaced (which is returned by queue function)
							ind=queue(countQueue[j],cacheL1[j],j,addressAfterSplit.get(i)[0],addressAfterSplit.get(i)[1]);
							//If the replaced index block is a dirty bit then we should write-back
							if(dirtyBits[j][ind])
							{
								//Increment L1 write-back
								L1writeBacks++;
								//Write the replaced block to L2 if the L2 associativity is mentioned
								if (L2Assoc!=0)
								{
									L2write(cacheL1[j][ind],j,i);
								}
							}
							//Also read the original block (current instruction) to L2 (if the L2 associativity is mentioned)
							if (L2Assoc!=0)
				        	{
				        	   L2read(addressAfterSplit.get(i)[0],j,i);
				        	}
							//Set the dirty bits in the block and update the cache
							cacheL1[j][ind]=addressAfterSplit.get(i)[0];
							countOptimal[j][ind]=addressAfterSplit.get(i)[0];
							countOptimalAdd[j][ind]=addressAfterSplit.get(i)[0];
							//Update the queue (FIFO) counter
							countQueue[j][ind]=(Arrays.stream(countQueue[j]).max().getAsInt())+1;
							dirtyBits[j][ind]=true;
							break;
						case 2:
							//Get the index to be replaced (which is returned by optimal function)
							ind=optimalPolicy("L1",countOptimal[j],i,countOptimalAdd[j],j);
							//If the replaced index block is a dirty bit then we should write-back
							if(dirtyBits[j][ind])
							{
								//Increment L1 write-back
								L1writeBacks++;
								//Write the replaced block to L2 if the L2 associativity is mentioned
								if (L2Assoc!=0)
								{
									L2write(cacheL1[j][ind],j,i);
								}
							}
							//Also read the original block (current instruction) to L2 (if the L2 associativity is mentioned)
							if (L2Assoc!=0)
				        	{
				        	   L2read(addressAfterSplit.get(i)[0],j,i);
				        	}
							//Set the dirty bits in the block and update the cache
							cacheL1[j][ind]=addressAfterSplit.get(i)[0];
							//Update the optimal array
							countOptimal[j][ind]=addressAfterSplit.get(i)[0];
							countOptimalAdd[j][ind]=addressAfterSplit.get(i)[0];
							dirtyBits[j][ind]=true;
							break;	  
				      }
				  }
			  }
		  }
	  }
}


public void L2read(String tagAdd, int indexAdd,int i) {
//	String tagBit=addressAfterSplitL2.get(addrIndex)[0];
	String addressBinary=hexToBinary(tagAdd, tagBitsCountL1)+decToBinary(indexAdd, indexBitsCountL1);
	String tagSplitL2=binaryToHex(addressBinary.substring(0, tagBitsCountL2));
	int indexSplitL2=Integer.valueOf(binaryToDec(addressBinary.substring(tagBitsCountL2, tagBitsCountL2+indexBitsCountL2)));
	int rep=0;
	int ind;
	L2reads++;
	  FirstInL2:
	  for(int j=0;j<cacheL2.length;j++)
	  {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
		  if(j!=indexSplitL2)
			  continue;
		  for(int k=0;k<cacheL2[j].length;k++)
		  {
			  if(Objects.isNull(cacheL2[j][k])||cacheL2[j][k].isEmpty()||cacheL2[j][k]==null)
			  {
				  L2readMiss++;
				  MemoryTraffic++;
				  cacheL2[j][k]=tagSplitL2;
				  countOptimalL2[j][k]=tagSplitL2;
//				  countOptimalAddL2[j][k]=address[addrIndex];
				  countQueueL2[j][k]=(Arrays.stream(countQueueL2[j]).max().getAsInt())+1;
				  countLRUL2[j][k]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
				  break FirstInL2;
			  }
			  else if(cacheL2[j][k].equalsIgnoreCase(tagSplitL2))
			  {
				  countLRUL2[j][k]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
				  break FirstInL2;
			  }
		  }
		  for(int k=0;k<cacheL2[j].length;k++)
		  {
			  if(!cacheL2[j][k].equalsIgnoreCase(tagSplitL2))
			  {
				  rep++;
			  }
		  }
		  if(rep==L2Assoc)
		  {
			  rep=0;
			  L2readMiss++;
			  MemoryTraffic++;
		      switch(replacementPolicy)
		      {
		        case 0:
		        	ind=leastRecentlyUsed(countLRUL2[j],cacheL2[j],j,tagSplitL2,String.valueOf(indexSplitL2));
		        	if(dirtyBitsL2[j][ind])
					{
		        		L2writeBacks++;
		        		MemoryTraffic++;
					}
		        	if (inclusionProperty!=0)
	        		{
	        			//call the inclusive function
	        			updateInclusivePropL1(cacheL2[j][ind],j);
	        		}
		        	dirtyBitsL2[j][ind]=false;
		        	cacheL2[j][ind]=tagSplitL2;
		        	countOptimalL2[j][ind]=tagSplitL2;
//		        	countOptimalAddL2[j][ind]=address[addrIndex];
					countLRUL2[j][ind]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
		        	break;
				case 1:
					ind=queue(countQueueL2[j],cacheL2[j],j,tagSplitL2,String.valueOf(indexSplitL2));
					if(dirtyBitsL2[j][ind])
					{
		        		L2writeBacks++;
		        		MemoryTraffic++;
					}
		        	if (inclusionProperty!=0)
	        		{
	        			//call the inclusive function
	        			updateInclusivePropL1(cacheL2[j][ind],j);
	        		}
					dirtyBitsL2[j][ind]=false;
					cacheL2[j][ind]=tagSplitL2;
					countOptimalL2[j][ind]=tagSplitL2;
//					countOptimalAddL2[j][ind]=address[addrIndex];
					countQueueL2[j][ind]=(Arrays.stream(countQueueL2[j]).max().getAsInt())+1;
					break;
				case 2:
					ind=optimalPolicy("L2",countOptimalL2[j],i,countOptimalAddL2[j],j);
					if(dirtyBitsL2[j][ind])
					{
		        		L2writeBacks++;
		        		MemoryTraffic++;
					}
		        	if (inclusionProperty!=0)
	        		{
	        			//call the inclusive function
	        			updateInclusivePropL1(cacheL2[j][ind],j);
	        		}
					cacheL2[j][ind]=tagSplitL2;
					countOptimalL2[j][ind]=tagSplitL2;
					countOptimalAddL2[j][ind]=tagSplitL2;
					dirtyBitsL2[j][ind]=false;
					break;
		      }
		  }
	  }
}
			
		

public void L2write(String tagAdd,int indexAdd,int addrIndex) 
{
	int rep=0;
	int ind;
	//Get the complete address to convert L1 tag and index to L2 tag and index
	String addressBinary=hexToBinary(tagAdd, tagBitsCountL1)+decToBinary(indexAdd, indexBitsCountL1);
	String tagSplitL2=binaryToHex(addressBinary.substring(0, tagBitsCountL2));
	int indexSplitL2=Integer.valueOf(binaryToDec(addressBinary.substring(tagBitsCountL2, tagBitsCountL2+indexBitsCountL2)));
//	String tagSplitL2=addressAfterSplitL2.get(addrIndex)[0];
//	int indexSplitL2=Integer.valueOf(addressAfterSplitL2.get(addrIndex)[1]);
	
	
	//Increment L2 writes
	L2writes++;
	
	  //The whole logic of L2 write is similar to L1 write
      //The only difference is that instead of iterating through the whole instructions
	  //We only perform the operation to the address that is returned from L1 
	  FirstInL2:
	  for(int j=0;j<cacheL2.length;j++)
	  {
		  if(j!=indexSplitL2)
		  {
			  continue;
		  }
		  for(int k=0;k<cacheL2[j].length;k++)
		  {
			  if(Objects.isNull(cacheL2[j][k])||cacheL2[j][k].isEmpty()||cacheL2[j][k]==null)
			  {
				  L2writeMiss++;
				  MemoryTraffic++;
				  cacheL2[j][k]=tagSplitL2;
				  countOptimalL2[j][k]=tagSplitL2;
				  countOptimalAddL2[j][k]=address[addrIndex];
				  countQueueL2[j][k]=(Arrays.stream(countQueueL2[j]).max().getAsInt())+1;
				  countLRUL2[j][k]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
				  dirtyBitsL2[j][k]=true;
				  break FirstInL2;
			  }
			  else if(cacheL2[j][k].equalsIgnoreCase(tagSplitL2))
			  {
				  countLRUL2[j][k]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
				  dirtyBitsL2[j][k]=true;
				  break FirstInL2;
			  }
		  }
		  for(int k=0;k<cacheL2[j].length;k++)
		  {
			  if(!cacheL2[j][k].equalsIgnoreCase(tagSplitL2))
			  {
				  rep++;
			  }
		  }
		  if(rep==L2Assoc)
		  {
			  rep=0;
			  L2writeMiss++;
			  MemoryTraffic++;
		      switch(replacementPolicy)
		      {
		        case 0:
		        	ind=leastRecentlyUsed(countLRUL2[j],cacheL2[j],j,tagSplitL2,String.valueOf(indexSplitL2));
		        	if(dirtyBitsL2[j][ind])
					{
		        		L2writeBacks++;
		        		MemoryTraffic++;
					}
		        	//if the inclusive property is specified
					//then we should remove the block that is getting replaced in L2, in L1 as well
		        	if (inclusionProperty!=0)
	        		{
	        			//call the inclusive function
	        			updateInclusivePropL1(cacheL2[j][ind],j);
	        		}
		        	cacheL2[j][ind]=tagSplitL2;
		        	countOptimalL2[j][ind]=tagSplitL2;
		        	countOptimalAddL2[j][ind]=tagSplitL2;
					countLRUL2[j][ind]=(Arrays.stream(countLRUL2[j]).max().getAsInt())+1;
		        	dirtyBitsL2[j][ind]=true;
		        	break;
				case 1:
					ind=queue(countQueueL2[j],cacheL2[j],j,tagSplitL2,String.valueOf(indexSplitL2));
					if(dirtyBitsL2[j][ind])
					{
						L2writeBacks++;
						MemoryTraffic++;
					}
					//if the inclusive property is specified
					//then we should remove the block that is getting replaced in L2, in L1 as well
					if(inclusionProperty!=0)
					{
						//call the inclusive function
						updateInclusivePropL1(cacheL2[j][ind],j);
					}
					cacheL2[j][ind]=tagSplitL2;
					countOptimalL2[j][ind]=tagSplitL2;
					countOptimalAddL2[j][ind]=tagSplitL2;
					countQueueL2[j][ind]=(Arrays.stream(countQueueL2[j]).max().getAsInt())+1;
					dirtyBitsL2[j][ind]=true;
					break;
				case 2:
					ind=optimalPolicy("L2",countOptimalL2[j],addrIndex,countOptimalAddL2[j],j);
					if(dirtyBitsL2[j][ind])
					{
						L2writeBacks++;
						MemoryTraffic++;
					}
					//if the inclusive property is specified
					//then we should remove the block that is getting replaced in L2, in L1 as well
					if (inclusionProperty!=0)
					{
						//call the inclusive function
						updateInclusivePropL1(cacheL2[j][ind],j);
					}
					cacheL2[j][ind]=tagSplitL2;
					countOptimalL2[j][ind]=tagSplitL2;
					countOptimalAddL2[j][ind]=tagSplitL2;
					dirtyBitsL2[j][ind]=true;
					break;
		      }
		  }
	  }
}

public void updateInclusivePropL1(String tagAddL2, int indexAddL2)
{
	//getting the tag and index bit that is getting replaced in L2
	//converting those tag and index bits as per L1
	String addressBinary=hexToBinary(tagAddL2, tagBitsCountL2)+decToBinary(indexAddL2, indexBitsCountL2);
	String tagSplitL1=binaryToHex(addressBinary.substring(0, tagBitsCountL1));
	int indexSplitL1=Integer.valueOf(binaryToDec(addressBinary.substring(tagBitsCountL1, tagBitsCountL1+indexBitsCountL1)));
	for(int j=0;j<cacheL1.length;j++)
	  {
		  if(j!=indexSplitL1)
		  {
			  continue;
		  }
		  repL1:
		  for(int k=0;k<cacheL1[j].length;k++)
		  {
			  //Getting the block
			  if(Objects.isNull(cacheL1[j][k])||cacheL1[j][k].isEmpty())
			  {
				  continue repL1;
			  }
			  //if it is a hit then replace that block with an empty string and remove the dirty block
			  if(cacheL1[j][k].equalsIgnoreCase(tagSplitL1))
			  {
				  if(dirtyBits[j][k])
				  { MemoryTraffic++;}
				  cacheL1[j][k]="";
				  dirtyBits[j][k]=false;
				  return;
			  }
		  }
	  }
}

//Function to get maximum index in the array
int getMaxIndex(int[] arr)
{
	int max=arr[0];
	int maxIndex=0;
	for (int i=0; i<arr.length; i++) 
	{
		if (max<arr[i]) 
		{
			max=arr[i];
			maxIndex=i;
		}
	}
	return maxIndex;
}

//Function to get minimum value in the array
int getMinVal(int[] arr)
{
	int min=arr[0];
	int minIndex=0;
	for (int i=0; i<arr.length; i++) 
	{
		if (min>arr[i]) 
		{
			min=arr[i];
			minIndex=i;
		}
	}
	return min;
}

//Function to get minimum index in the array
int getMinInd(int[] arr)
{
	int min=arr[0];
	int minIndex=0;
	for (int i=0; i<arr.length; i++) 
	{
		if (min>arr[i]) 
		{
			min=arr[i];
			minIndex=i;
		}
	}
	return minIndex;
}
int optimalPolicy(String cacheLevel, String[] setVals, int addressInt, String[] setValAddresses,int indexBitNo) 
{
	//Optimal function logic: the Optimal array is the duplicate of the whole cache
	//We will get the contents in the set that we are currently accessing
	//and check the index of each address in the set appeared after the current instruction that is being accessed
	List<String[]> subAddressAfterSplit;
	int[] subArrInt;
	if(cacheLevel.equalsIgnoreCase("L2"))
	{
		subAddressAfterSplit=addressAfterSplitL2.subList(addressInt+1, address.length);
		subArrInt=new int[L2Assoc];
	}
	else
	{
		subAddressAfterSplit=addressAfterSplit.subList(addressInt+1, address.length);
		subArrInt=new int[L1Assoc];
	}
	nextAddr:
	for(int i=0;i<setVals.length;i++)
	{
		for(int j=0;j<subAddressAfterSplit.size();j++)
		{
			if(subAddressAfterSplit.get(j)[0].equalsIgnoreCase(setVals[i]) && subAddressAfterSplit.get(j)[1].equalsIgnoreCase(String.valueOf(indexBitNo)))
			{
				subArrInt[i]=j;
				continue nextAddr;
			}
			if(j==subAddressAfterSplit.size()-1 && subArrInt[i]==0)
			{
				subArrInt[i]=address.length;
			}
		}
	}
	//we will get the highest index of the set (which is accessed far in the future)
	//if there is no appearance of the address in the future at all
	//then its index is replaced by the length of the instruction list (which will clearly be the one to be replaced)
	int maxAddIndex=getMaxIndex(subArrInt);
	//Returning the index accessed far in the future
	return maxAddIndex;
}

int queue(int[] queueCountArr, String[] cacheL1Temp, int arrIndex, String tagAdd, String indexAdd) 
{
	//FIFO function logic: the FIFO counter array has the counts of each block (incremented only when it is a miss)
	int min=queueCountArr[0];
	int minIndex=0;
	for (int i=0; i<queueCountArr.length; i++) 
	{
		if (min>queueCountArr[i]) 
		{
			min=queueCountArr[i];
			minIndex=i;
		}
	}
	//Returns the block with minimum number of increments (which was inserted first)
	return minIndex;
}

int leastRecentlyUsed(int[] LRUCountArr, String[] cacheL1Temp, int arrIndex, String tagAdd, String indexAdd) 
{
	//LRU function logic: the LRU counter array has the counts of each block (incremented when it is a hit and miss)
	int min=LRUCountArr[0];
	int minIndex=0;
	for (int i=0;i<LRUCountArr.length;i++) 
	{
		if (min>LRUCountArr[i]) 
		{
			min=LRUCountArr[i];
			minIndex=i;
		}
	}
	//it returns the block which has minimum number of increments (which was least used)
	return minIndex;
}
void checkValidation()
{
	//Checking if the given replacement policy and inclusion property are valid
	if(replacementPolicy!=0&&replacementPolicy!=1&&replacementPolicy!=2) {System.out.println("Invalid replacement policy");System.exit(0);}
	if(inclusionProperty!=0&&inclusionProperty!=1) {System.out.println("Invalid inclusion property");System.exit(0);}
}
void displayResults() 
{
	String repPolicy="";
	String incProp="";
	if(replacementPolicy==0)repPolicy="LRU";
	else if(replacementPolicy==1)repPolicy="FIFO";
	else if(replacementPolicy==2)repPolicy="optimal";
	if(inclusionProperty==0)incProp="non-inclusive";
	else if(inclusionProperty==1)incProp="inclusive";
	
	//Printing Simulator configuration
	System.out.print("===== Simulator configuration =====\r\n");
	System.out.print("BLOCKSIZE:             "+blockSize+"\r\n" + 
			"L1_SIZE:               "+L1Size+"\r\n" + 
			"L1_ASSOC:              "+L1Assoc+"\r\n" + 
			"L2_SIZE:               "+L2Size+"\r\n" + 
			"L2_ASSOC:              "+L2Assoc+"\r\n" + 
			"REPLACEMENT POLICY:    "+repPolicy+"\r\n" + 
			"INCLUSION PROPERTY:    "+incProp+"\r\n" + 
			"trace_file:            "+traceFile+"\r\n");
	//Printing L1 Cache contents
	System.out.print("===== L1 contents =====\r\n");
	for(int i=0;i<cacheL1.length;i++)
	  {
	  System.out.print("Set     "+i+":      ");
	  for(int j=0;j<cacheL1[i].length;j++)
	  {
		  if(dirtyBits[i][j])
			  System.out.print(cacheL1[i][j]+" D  ");
		  else
			  System.out.print(cacheL1[i][j]+"    ");
	  }
	  System.out.print("\r\n");
	  }
	  //Printing L2 cache contents if the L2 associativity is given
	  if(L2Assoc!=0)
	  {
	  System.out.print("===== L2 contents =====\r\n");
	  for(int i=0;i<cacheL2.length;i++)
	  {System.out.print("Set     "+i+":      ");
	  for(int j=0;j<cacheL2[i].length;j++)
	  {
		  if(dirtyBitsL2[i][j])
			  System.out.print(cacheL2[i][j]+" D  ");
		  else
			  System.out.print(cacheL2[i][j]+"    ");
	  }
	  System.out.print("\r\n");
	  }
	  }
	  DecimalFormat df=new DecimalFormat("#0.000000");
	  float L1missRate=(float)(L1readMiss+L1writeMiss)/(L1reads+L1writes);
	  int missRateL2=0;
	  float L2missRate=((L2Assoc==0) ? 0 : (float)(L2readMiss)/(L2reads));
	  int totalMemoryTraffic=((L2Assoc==0) ? L1readMiss+L1writeMiss+L1writeBacks : MemoryTraffic);
	  //Printing simulation results
	  if(L2Assoc==0)
	  {
	  System.out.print("===== Simulation results (raw) =====\r\n" + 
	  		"a. number of L1 reads:        "+L1reads+"\r\n" + 
	  		"b. number of L1 read misses:  "+L1readMiss+"\r\n" + 
	  		"c. number of L1 writes:       "+L1writes+"\r\n" + 
	  		"d. number of L1 write misses: "+L1writeMiss+"\r\n" + 
	  		"e. L1 miss rate:              "+df.format(L1missRate)+"\r\n" + 
	  		"f. number of L1 writebacks:   "+L1writeBacks+"\r\n" + 
	  		"g. number of L2 reads:        "+L2reads+"\r\n" + 
	  		"h. number of L2 read misses:  "+L2readMiss+"\r\n" + 
	  		"i. number of L2 writes:       "+L2writes+"\r\n" + 
	  		"j. number of L2 write misses: "+L2writeMiss+"\r\n" + 
	  		"k. L2 miss rate:              "+missRateL2+"\r\n" + 
	  		"l. number of L2 writebacks:   "+L2writeBacks+"\r\n" + 
	  		"m. total memory traffic:      "+totalMemoryTraffic+"\r\n" + 
	  		"");
	  }
	  else
	  {
		  System.out.print("===== Simulation results (raw) =====\r\n" + 
			  		"a. number of L1 reads:        "+L1reads+"\r\n" + 
			  		"b. number of L1 read misses:  "+L1readMiss+"\r\n" + 
			  		"c. number of L1 writes:       "+L1writes+"\r\n" + 
			  		"d. number of L1 write misses: "+L1writeMiss+"\r\n" + 
			  		"e. L1 miss rate:              "+df.format(L1missRate)+"\r\n" + 
			  		"f. number of L1 writebacks:   "+L1writeBacks+"\r\n" + 
			  		"g. number of L2 reads:        "+L2reads+"\r\n" + 
			  		"h. number of L2 read misses:  "+L2readMiss+"\r\n" + 
			  		"i. number of L2 writes:       "+L2writes+"\r\n" + 
			  		"j. number of L2 write misses: "+L2writeMiss+"\r\n" + 
			  		"k. L2 miss rate:              "+df.format(L2missRate)+"\r\n" + 
			  		"l. number of L2 writebacks:   "+L2writeBacks+"\r\n" + 
			  		"m. total memory traffic:      "+totalMemoryTraffic+"\r\n" + 
			  		"");
	  }
}

//Function to convert decimal to binary
public String decToBinary(int decNum,int totalBits)
{
	return String.format("%"+totalBits+"s",Integer.toBinaryString(decNum)).replaceAll(" ", "0");
}

//Function to convert Hexadecimal to binary 
public String hexToBinary(String hexCode, int totalBits) 
{
	return String.format("%"+totalBits+"s",Integer.toBinaryString(Integer.parseInt(hexCode, 16))).replaceAll(" ", "0");
	  
}

//Function to convert Binary to Hexadecimal
public String binaryToHex(String binaryCode) 
{
	return Integer.toString(Integer.parseInt(binaryCode,2),16);
}

//Function to convert Binary to Decimal
public String binaryToDec(String binaryCode) 
{
	return Integer.toString(Integer.parseInt(binaryCode,2));
}
	
}