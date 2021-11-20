package block_chain;
import java.util.ArrayList;
import java.util.HashMap;


public class BlockChain {
    private HashMap<ByteArrayWrapper, BlockNode> blockChain;
    private BlockNode maxHeightBlockNode;
    private TransactionPool txPool;
    public static final int CUT_OFF_AGE = 10;
    
    private class BlockNode {
        public Block block;
        public int height;
        public UTXOPool utxoPool;
        public BlockNode parentNode;
        public ArrayList<BlockNode> childrenNodes;
        
        public BlockNode(Block block, BlockNode parentNode, UTXOPool utxoPool) {
            
        	this.block = block;
            this.parentNode = parentNode;
            this.utxoPool = utxoPool;
            this.childrenNodes = new ArrayList<>();
          
            if (parentNode == null) {
                this.height = 1;
            } else {
                this.height = parentNode.height + 1;
                this.parentNode.childrenNodes.add(this);
            }
        }
    }

    public BlockChain(Block genesisBlock) {
       
    	this.blockChain = new HashMap<>();
        UTXOPool utxoPool = new UTXOPool();

        for (int i = 0; i < genesisBlock.getCoinbase().getOutputs().size(); i++) {
            UTXO utxo = new UTXO(genesisBlock.getCoinbase().getHash(), i);
            utxoPool.addUTXO(utxo, genesisBlock.getCoinbase().getOutput(i));
        }
        
        BlockNode genesisBlockNode = new BlockNode(genesisBlock, null, utxoPool);
        blockChain.put(new ByteArrayWrapper(genesisBlock.getHash()), genesisBlockNode);
        this.txPool = new TransactionPool();
        
        this.maxHeightBlockNode = genesisBlockNode;
    }

    public Block getMaxHeightBlock() {
        return maxHeightBlockNode.block;
    }

    public UTXOPool getMaxHeightUTXOPool() {
        return maxHeightBlockNode.utxoPool;
    }

    public TransactionPool getTransactionPool() {
        return txPool;
    }

   
    public boolean addBlock(Block block) {
        if (block == null) {
            return false;
        }
            
        if (block.getPrevBlockHash() == null) {
            return false;
        }
        
        BlockNode blockParentNode = blockChain.get(new ByteArrayWrapper(block.getPrevBlockHash()));

        if (blockParentNode == null) {
            return false;
        }

        TxHandler txHandler = new TxHandler(blockParentNode.utxoPool);
        Transaction[] blockTxs = new Transaction[block.getTransactions().size()];
        for (int i = 0; i < block.getTransactions().size(); i++) {
            blockTxs[i] = block.getTransaction(i);
        }
        Transaction[] validTxs = txHandler.handleTxs(blockTxs);
        
        if (validTxs.length != blockTxs.length) {
            return false;
        }
        
	  int proposedHeight = (blockParentNode .height + 1);
        if (proposedHeight <= maxHeightBlockNode.height - CUT_OFF_AGE) {
            return false;
        }
 
        for (int i = 0; i < block.getCoinbase().getOutputs().size(); i++) {
            UTXO utxo = new UTXO(block.getCoinbase().getHash(), i);
            txHandler.getUTXOPool().addUTXO(utxo, block.getCoinbase().getOutput(i));
        }
        BlockNode blockNode = new BlockNode(block, blockParentNode, txHandler.getUTXOPool());
        blockChain.put(new ByteArrayWrapper(block.getHash()), blockNode);
        
     
          if (proposedHeight > maxHeightBlockNode.height) {
            maxHeightBlockNode = blockNode ;
        }

          if (maxHeightBlockNode.height > CUT_OFF_AGE + 1) {
                      for (HashMap.Entry<ByteArrayWrapper, BlockNode> entry : blockChain.entrySet()) {
                          BlockNode blockNode1 = entry.getValue();
                          if (blockNode1.height <= maxHeightBlockNode.height - CUT_OFF_AGE) {
                              for (int i = 0; i < blockNode1.childrenNodes.size(); i++) {
                                  blockNode1.childrenNodes.get(i).parentNode = null;
                              }
                              blockNode1.childrenNodes.clear();
                          }
                      }
                  }
          
        return true;
    }

    public void addTransaction(Transaction tx) {
        this.txPool.addTransaction(tx);
    }
}