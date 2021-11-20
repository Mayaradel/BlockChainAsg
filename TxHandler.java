package block_chain;

import java.util.ArrayList;


public class TxHandler {

	private UTXOPool utxoPool;
	UTXO utxo;
	double countInput = 0;
	double countOutput = 0;
	private ArrayList<UTXO> utxoList = new ArrayList<UTXO>();
	

	/**
	 * Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is {@code utxoPool}.
	 */
	public TxHandler(UTXOPool utxoPool) {
		this.utxoPool = new UTXOPool(utxoPool);
	}

	/**
	 * @return true if: (1) all outputs claimed by {@code tx} are in the current
	 *         UTXO pool, (2) the signatures on each input of {@code tx} are valid,
	 *         (3) no UTXO is claimed multiple times by {@code tx}, (4) all of
	 *         {@code tx}s output values are non-negative, and (5) the sum of
	 *         {@code tx}s input values is greater than or equal to the sum of its
	 *         output values; and false otherwise.
	 */
	public boolean isValidTx(Transaction tx) {

		for (int i = 0; i < tx.numInputs(); i++) {

			boolean flag = false;

			UTXO utxo = new UTXO(tx.getInput(i).prevTxHash, tx.getInput(i).outputIndex);

			for (int j = 0; j < utxoPool.getAllUTXO().size(); j++) {

				if (utxo.equals(utxoPool.getAllUTXO().get(j))) {
					countInput = utxoPool.getTxOutput(utxoPool.getAllUTXO().get(j)).value + countInput;
					flag = true;
					
					
					if (!Crypto.verifySignature(utxoPool.getTxOutput(utxoPool.getAllUTXO().get(j)).address,
							tx.getRawDataToSign(i), tx.getInput(i).signature)) {
						return false;
					}

					if (utxoList.contains(utxoPool.getAllUTXO().get(j)))
						return false;

					utxoList.add(utxoPool.getAllUTXO().get(j));

				}
			}
			if (flag == false)
				return false;

		}

		for (int k = 0; k < tx.numOutputs(); k++) {
			countOutput = tx.getOutput(k).value + countOutput;
			if (tx.getOutput(k).value < 0)
				return false;
		}

		if (countOutput > countInput)
			return false;

		return true;

	}

	
	 public Transaction[] handleTxs(Transaction[] possibleTxs) {
	        
		 ArrayList<Transaction> validTransactions = new ArrayList<>();
	      
		 for (int j=0 ; j<possibleTxs.length;j++) {
			 
	            if (isValidTx(possibleTxs[j])) {
	                validTransactions.add(possibleTxs[j]);

	                for (int k = 0; k < possibleTxs[j].numInputs(); k++) {
	                	Transaction.Input input = possibleTxs[j].getInput(k);
	                	int outputIndex = input.outputIndex;
	                    byte[] prevTxHash = input.prevTxHash;
	                    UTXO utxo = new UTXO(prevTxHash, outputIndex);
	                    utxoPool.removeUTXO(utxo);
	                }
	           
	                byte[] hash = possibleTxs[j].getHash();
	                for (int i=0;i<possibleTxs[j].numOutputs();i++) {
	                    UTXO utxo = new UTXO(hash, i);
	                    utxoPool.addUTXO(utxo, possibleTxs[j].getOutput(i));
	                }
	            }
	        }
	        Transaction[] validTransactionsArr = new Transaction[validTransactions.size()];
	        validTransactionsArr = validTransactions.toArray(validTransactionsArr);
	        return validTransactionsArr;
	    }
	 
	 
	
	public UTXOPool getUTXOPool () {
		return utxoPool;
		
	}

}
