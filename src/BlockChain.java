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
   private BlockNode genesisblock;
   private BlockNode prevProcessedBlock;
   
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
      genesisblock = genesis;
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
   
   public BlockNode getPrevProcessedBlock() {
	   return prevProcessedBlock;
   }
   public int getHeight() {
	   return height;
   }
   public BlockNode getGenesisBlockNode() {
	   return genesisblock;
   }
   /* Get the UTXOPool for mining a new block on top of 
    * max height block
    */
   public UTXOPool getMaxHeightUTXOPool() {
	   return maxHeightBlock.uPool;
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
	   
	   /* list of things to check:
	    * 1. B is genesis block (if yes, return false) 
	    * 		-- b.getPrevBlockHash() == null
	    * 2. B's parent is null (if yes, return false) 
	    * 		-- H.get(new ByteArrayWrapper(b.getPrevBlockHash())) == null
	    * 3. Transactions are valid (if invalid, return false)
	    * 4. if height > CUT_OFF_AGE + 1 (if yes, return false)
	    */
	  
	   /* Check 1: B is a genesis block */
	   if (b.getPrevBlockHash() == null) {
		   return false;
	   }

	   /* Check 2: B's parent is null */
	   BlockNode bParent = H.get(new ByteArrayWrapper(b.getPrevBlockHash()));
	   if (bParent == null) {
		   return false; 
	   }

	   /*use handleTxs to detect double spends and invalid Txs
	    * if validTx == bTx, then block is valid and should be added.
	   */
	   Transaction bTx[] = b.getTransactions().toArray(new Transaction[0]);
	   
	   //TxHandler handlemytx = new TxHandler(getMaxHeightUTXOPool()); 
	   //b's transactions must be based on parent's UTXO pool, not the max height UTXO pool
	   UTXOPool bParentUTXO = bParent.getUTXOPoolCopy();
	   TxHandler handlemytx = new TxHandler(bParentUTXO);
	   Transaction validTx[] = handlemytx.handleTxs(bTx);   
	   
	   if (/*bTx.length > 0 &&*/ !Arrays.equals(bTx, validTx)) {
		   return false;
	   }
	   /* return false if invalid prevBlockHash detected
	    * plan: check all blocknode hashes
	    */
	   /*
	   else if (b.getPrevBlockHash() != getGenesisBlockNode().b.getHash()) {		   
		   if (b.getPrevBlockHash() != getMaxHeightBlock().getHash()) {
			   if (H.get(b.getPrevBlockHash()) == prevProcessedBlock) {
				   //return false;
			   }
			   
			   boolean hashMatch = false;
			   for (BlockNode childBlock : getGenesisBlockNode().children) {
				   if (childBlock.children.size() > 0) {
					   for (BlockNode childChildBlock : childBlock.children) {
						   if (childChildBlock.children.size() == 0) {
							   if (b.getPrevBlockHash() == childChildBlock.b.getHash()) {
								   hashMatch = true; 
								   break;
							   }
						   }
					   }
				   }
				   else if (b.getPrevBlockHash() == childBlock.b.getHash() || hashMatch == true) {
					   hashMatch = true;
					   break;
				   }
			   }		   
			   if (hashMatch == false) return false;
			   
		   }		   
	   }
		*/
	   /* steps to add a block
	    * make blocknode with block
	    * update height, maxheightblock
	    * add to hash
	    */
	   //BlockNode currMaxHeightBN = getMaxHeightBlockNode();
	   //BlockNode newBN = new BlockNode(b, currMaxHeightBN, getMaxHeightUTXOPool());
	   BlockNode newBN = new BlockNode(b, bParent, handlemytx.getUTXOPool());
	   //currMaxHeightBN.b.finalize();
	   //newBN.b.finalize();
	   //if (height > CUT_OFF_AGE + 1 && newBN.b.getPrevBlockHash() == getGenesisBlockNode().b.getHash()) return false;

	   H.put(new ByteArrayWrapper(b.getHash()), newBN);
	   if (newBN.height > height) {
		   maxHeightBlock = newBN;
		   height = newBN.height;
	   }
	   //this.prevProcessedBlock = newBN;
	   
	   return true;
   }

   /* Add a transaction in transaction pool
    */
   public void addTransaction(Transaction tx) {
    	  txPool.addTransaction(tx);
   }
}