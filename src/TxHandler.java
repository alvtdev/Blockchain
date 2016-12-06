import java.util.ArrayList;

public class TxHandler {

	private UTXOPool pool;
	
	/* Creates a public ledger whose current UTXOPool (collection of unspent 
	 * transaction outputs) is utxoPool. This should make a defensive copy of 
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		pool = new UTXOPool(utxoPool);
	}

	/* Returns true if 
	 * (1) all outputs claimed by tx are in the current UTXO pool,  
	 * (2) the signatures on each input of tx are valid, 
	 * (3) no UTXO is claimed multiple times by tx, 
	 * (4) all of tx's output values are non-negative, and
	 * (5) the sum of tx's input values is greater than or equal to the sum of   
	        its output values;
	   and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {
		// List of all inputs and outputs in this transaction
		ArrayList<Transaction.Input> inputs = tx.getInputs();
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		//List of all UTXOs in current pool 
		ArrayList<UTXO> UTXOs = pool.getAllUTXO();
		//List of all UTXOs that were previously seen (to avoid multiple claims)
		ArrayList<UTXO> prevUTXOs = new ArrayList<UTXO>();
		//Variables to track input and output vales 
		double inValue = 0.0;
		double outValue = 0.0;
		
		int index = 0;
		for (Transaction.Input i : inputs) {
			//create UTXO item given previous hash and index from input
			UTXO currUTXO = new UTXO(i.prevTxHash, i.outputIndex);
			
			//check previously checked UTXOs. If seen, multiple claim -> return false
			if (prevUTXOs.contains(currUTXO)) return false;
			
			//Assuming currUTXO was not seen, add to list of previously seen UTXOs
			prevUTXOs.add(currUTXO);
			
			//check if currUTXO is in the current pool. if not, return false;
			if (!UTXOs.contains(currUTXO)) return false;
			
			/*check for valid signature and data
			 * getTxOutput returns a value and RSA address
			 * use RSA verifySignature(message, signature) on RSA address - found in RSA API
			 * tx.getRawDataToSign = message to sign
			 * index = index of byte in byte[]??? 
			 * signature = i.signature
			 */
			if (!pool.getTxOutput(currUTXO).address.verifySignature(tx.getRawDataToSign(index), i.signature)) return false;
			index++;
			
			//increment input value counter
			inValue += pool.getTxOutput(currUTXO).value;			
		}
		
		for (Transaction.Output o : outputs) {
			//check output value, if negative return false 
			if (o.value < 0) return false;
			
			//increment output value
			outValue += o.value;
		}
		
		//return false if sum of input value < sum of output value
		if (inValue < outValue) return false;

		return true;
	}

	/* Handles each epoch by receiving an unordered array of proposed 
	 * transactions, checking each transaction for correctness, 
	 * returning a mutually valid array of accepted transactions, 
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		//Arrays containing valid/invalid transactions
		ArrayList<Transaction> validTx = new ArrayList<Transaction>();
		ArrayList<Transaction> invalidTx = new ArrayList<Transaction>();
		//Variables:
		int posTxLen = possibleTxs.length; //stores size of possibleTxs
		
		//loop: check each Tx in possibleTxs for validity
		for(int i = 0; i < posTxLen; i++) {
			
			//if the Tx is valid
			if (isValidTx(possibleTxs[i])) {
				/* -find old UT and remove from pool-
				 * create new UTXO item using prevTxHash and outputIndex
				 * Remove that UTXO from the pool
				 */
				for (int j = 0; j < possibleTxs[i].numInputs(); j++) {
					UTXO oldUTx = new UTXO(possibleTxs[i].getInput(j).prevTxHash, possibleTxs[i].getInput(j).outputIndex);
					pool.removeUTXO(oldUTx);
				}
				
				/* After removing the old UTXO,
				 * create a new UTXO, and add the new UTXO to the UTXOpool
				 * also add Tx to the validTx list.
				 */
				for (int j = 0; j < possibleTxs[i].numOutputs(); j++) {
					UTXO newUTx = new UTXO(possibleTxs[i].getHash(), j);
					pool.addUTXO(newUTx, possibleTxs[i].getOutput(j));
				}
				
				validTx.add(possibleTxs[i]);
			}
			//else (the Tx is invalid)
			else {
				//add Tx to the invalid Tx list
				invalidTx.add(possibleTxs[i]);
			}
		}
		
		/*Since some transactions may be dependent on others,
		 * continually loop through the invalidTx list to find them.
		 * Stop looping once all dependent transactions have been found.
		 */
		
		int validTxCount = 0;
		do {
			validTxCount = 0;
			for (Transaction t : invalidTx) {
				
				//if a valid tx is found
				if (isValidTx(t)) {
					//remove from invalid list and add to valid list
					invalidTx.remove(t);
					validTx.add(t);
					
					//remove old UTXO from the UTXOpool					
					for (Transaction.Input in : t.getInputs()) {
						UTXO oldUTx = new UTXO(in.prevTxHash, in.outputIndex);
						pool.removeUTXO(oldUTx);
					}
					
					//add new UTXO to the UTXOpool
					for (int i = 0; i < t.numOutputs(); i++) {
						UTXO newUT = new UTXO(t.getHash(), i);
						pool.addUTXO(newUT, t.getOutput(i));
					}
					
					//increment counter
					validTxCount += 1 ;
				}
			}
		} while (validTxCount > 0);
		
		return validTx.toArray(new Transaction[0]);
	}
	
	/* Returns the current UTXO pool.If no outstanding UTXOs, returns an empty (non-null) UTXOPool object. */
	public UTXOPool getUTXOPool() {
		return pool;
	}

} 