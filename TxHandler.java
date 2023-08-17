import java.util.ArrayList;
import java.util.List;
public class TxHandler {
    private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }


    public UTXOPool geUtxoPool(){
        return utxoPool;
    }
    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {   
        return (InputsAreInPool(tx) 
                && InputsSignatureValid(tx)
                && InputsClaimedMultiple(tx) 
                && OutputsNotNegatvie(tx) 
                && TransactionFee(tx));
         }

        // Objective 1:In Pool
        private boolean InputsAreInPool(Transaction tx){
            boolean validationtx = true;
            for (int i = 0; i < tx.numInputs(); i++) {
                Transaction.Input in = tx.getInput(i);
                if (in != null) {
                    UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
                    if (!this.utxoPool.contains(u)) {
                        validationtx = false;;
                    } 
                }
            }
            return validationtx;
        }
      
        //Objective 2: Valid Inputs
        private boolean InputsSignatureValid(Transaction tx){
            boolean validationtx = true;
            for (int i = 0; i < tx.numInputs(); i++) {
                Transaction.Input in = tx.getInput(i);
                if (in != null) {
                    UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
                    Transaction.Output op = this.utxoPool.getTxOutput(u);
				    if (!Crypto.verifySignature(op.address, tx.getRawDataToSign(i), in.signature)) {
	    			validationtx = false;
                    }
                }
            }
            return validationtx; 
        }

        // Objective 3: Claimed Multiple Times
        private boolean InputsClaimedMultiple(Transaction tx){
            boolean validationtx = true;
            List<UTXO> utxoList = new ArrayList<UTXO>();
    		for (Transaction.Input in: tx.getInputs()) {
        		UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
        		utxoList.add(u);
    		}
			outerLoop:
			for (int i = 0; i < utxoList.size(); i++) {
				UTXO curr = utxoList.get(i);
				for (int j = i+1; j < utxoList.size(); j++) {
					if (curr.equals(utxoList.get(j))) {
						validationtx = false;
						break outerLoop;
					}
				}
			}
            return validationtx;
            } 

        // Objective 4: Non-negative
        private boolean OutputsNotNegatvie(Transaction tx){
            boolean validationtx = true;
            for (Transaction.Output output: tx.getOutputs()){
                if (output.value <= 0)
                    validationtx = false;
            }
            return validationtx;
        }
        // Objective 5: Transaction Fee
        private boolean TransactionFee(Transaction tx){
            double sumInputValues = 0;
    		double sumOutputValues = 0;
            boolean validationtx = true;
    		for (Transaction.Input in: tx.getInputs()) {
        		UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
        		Transaction.Output op = this.utxoPool.getTxOutput(u);
    			sumInputValues += op.value;
    		}
			for (Transaction.Output op: tx.getOutputs()) {
				sumOutputValues += op.value;
    		}
    		if (sumInputValues < sumOutputValues) {
    			validationtx = false;
    		}
            return validationtx;
    	}
    	  
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        List<Transaction> acceptedTxs = new ArrayList<Transaction>();
    	for (Transaction tx: possibleTxs) {
    		if (isValidTx(tx)) {
    			acceptedTxs.add(tx);
    			for (Transaction.Input in: tx.getInputs()) {
    				UTXO u = new UTXO(in.prevTxHash, in.outputIndex);
    				this.utxoPool.removeUTXO(u);
    			}
    			for (int i = 0; i < tx.numOutputs(); i++) {
    				Transaction.Output op = tx.getOutput(i);
    				if (op != null) {
	    				UTXO uNew = new UTXO(tx.getHash(), i);
	    				this.utxoPool.addUTXO(uNew, op);
    				}
    			}
    		}
    	}
    	return acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);
    }

    }
