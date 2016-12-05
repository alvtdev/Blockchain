import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/* Block Chain should maintain only limited block nodes to satisfy the functions
   You should not have the all the blocks added to the block chain in memory 
   as it would overflow memory
 */

public class BlockChain {
   public static final int CUT_OFF_AGE = 10;

   // all information required in handling a block in block chain
   private class BlockNode {
      public Block b;
      public BlockNode parent;
      public ArrayList<BlockNode> children;
      public int height;
      // utxo pool for making a new block on top of this block
      private UTXOPool uPool;

      public BlockNode(Block b, BlockNode parent, UTXOPool uPool) {
         this.b = b;
         this.parent = parent;
         children = new ArrayList<BlockNode>();
         this.uPool = uPool;
         if (parent != null) {
            height = parent.height + 1;
            parent.children.add(this);
         } else {
            height = 1;
         }
      }

      public UTXOPool getUTXOPoolCopy() {
         return new UTXOPool(uPool);
      }
   }

/* member variables */
   private ArrayList<BlockNode> heads;
   private HashMap<ByteArrayWrapper, BlockNode> H;
   private int height;
   private BlockNode maxHeightBlock;
   private TransactionPool txPool;
   
   /* create an empty block chain with just a genesis block.
    * Assume genesis block is a valid block
    * (reference code provided)
    */
   public BlockChain(Block genesisBlock) {
      UTXOPool uPool = new UTXOPool();
      Transaction coinbase = genesisBlock.getCoinbase();
      UTXO utxoCoinbase = new UTXO (coinbase.getHash(), 0);
      uPool.addUTXO(utxoCoinbase, coinbase.getOutput(0));
      BlockNode genesis = new BlockNode (genesisBlock, null, uPool);
      
      heads = new ArrayList<BlockNode>();
      heads.add(genesis);
      H = new HashMap<ByteArrayWrapper, BlockNode>();
      H.put(new ByteArrayWrapper(genesisBlock.getHash()), genesis);
      
      height = 1;
      maxHeightBlock = genesis;
      txPool = new TransactionPool();
   }

   /* Get the maximum height block
    */
   public Block getMaxHeightBlock() {
	   return maxHeightBlock.b;
   }
   /* Get the maximum height blockNode
    */
   public BlockNode getMaxHeightBlockNode() {
	   return maxHeightBlock;
   }
   /* Get the UTXOPool for mining a new block on top of 
    * max height block
    */
   public UTXOPool getMaxHeightUTXOPool() {
	   return maxHeightBlock.getUTXOPoolCopy();
   }
   
   /* Get the transaction pool to mine a new block
    */
   public TransactionPool getTransactionPool() {
	   return txPool;
   }

   /* Add a block to block chain if it is valid.
    * For validity, all transactions should be valid
    * and block should be at height > (maxHeight - CUT_OFF_AGE).
    * For example, you can try creating a new block over genesis block 
    * (block height 2) if blockChain height is <= CUT_OFF_AGE + 1. 
    * As soon as height > CUT_OFF_AGE + 1, you cannot create a new block at height 2.
    * Return true if block is successfully added
    */
   public boolean addBlock(Block b) {
       // IMPLEMENT THIS
	   
	   /*Return false if:
	    * height > CUT_OFF_AGE
	    * block b is a genesis block (prevhash = null)
	    * prevBlockHash is invalid
	    */
	   if (height > CUT_OFF_AGE + 1) {
		   return false;
	   }
	   else if (b.getPrevBlockHash() == null) {
		   return false;
	   }	   
	   //ArrayList<Transaction> bTx = b.getTransactions();
	   Transaction bTx[] = b.getTransactions().toArray(new Transaction[0]);
	   TxHandler handlemytx = new TxHandler(getMaxHeightUTXOPool());
	   
	   /*use handleTxs to detect double spends and invalid Txs
	    * if validTx == bTx, then block is valid and should be added.
	   */
	   Transaction validTx[] = handlemytx.handleTxs(bTx);
	   
	   if (/*bTx.length > 0 && validTx.length > 0 &&*/ !Arrays.equals(bTx, validTx)) return false;
	   
	   /* return false if invalid prevBlockHash detected
	    */
	   else if (getMaxHeightBlock().getHash() != b.getPrevBlockHash()) {
		   return false;
	   }
	   /* steps to add a block
	    * make blocknode with block
	    * update height, maxheightblock
	    * add to hash
	    */
	   BlockNode currMaxHeightBN = getMaxHeightBlockNode();
	   BlockNode newBN = new BlockNode(b, currMaxHeightBN, getMaxHeightUTXOPool());
	   currMaxHeightBN.children.add(newBN);
	   currMaxHeightBN.b.finalize();
	   
	   H.put(new ByteArrayWrapper(b.getHash()), newBN);
	   maxHeightBlock = newBN;	   	   
	   
	   if (b.getTransactions().size() == 0) {
		   return true;
	   }
	   return true;
   }

   /* Add a transaction in transaction pool
    */
   public void addTransaction(Transaction tx) {
      if (txPool.getTransaction(tx.getHash()) == null) {
    	  txPool.addTransaction(tx);
      }
   }
}