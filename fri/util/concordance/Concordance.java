package fri.util.concordance;

import java.util.*;

import fri.util.collections.AggregatingHashtable;
import fri.util.observer.CancelProgressObserver;

/**
	<p>
	Search for concordant (identical) objects and blocks of objects in a List.
	Normally the List will contain text lines, but could be a file list, too.
	This class should help to find copy + paste source sections.
	</p><p>
	To use this class several things must/can be provided:
	<ul>
		<li>A List of objects where concordances should be detected.
			Mind that their toString() method will used to hash them in a Map
			(this is an implicite responsibility, see class LineWrapper).</li>
		<li>A ValidityFilter that can influence the concordance search
			by its return code from <code>isValid()</code> method. When
			returning null the Object is not used for blocking with others.
			The ValidityFilter can modify the Object, or return another one,
			e.g. it can trim text lines.</li>
	</ul>
	The result that can be retrieved from Concordance search is a Block List,
	every Block contains multiple occurences, every occurence contains multiple
	input objects ("parts"). Within one Block the "part" count is equal for all
	occurences.
	</p><p>
	The following line sequence
	<pre>
		aaa
		bbb
		ccc
		
		aaa
		bbb
		
		ccc
	</pre>
	will be blocked as
	<pre>
		1: aaa
		2: bbb
		------
		5: aaa
		6: bbb
		======
		3: ccc
		------
		8: ccc
		======
	</pre></p>
	
	@author Fritz Ritzberger, 2003
*/

public class Concordance
{
	private static final boolean MAXIMIZE_BLOCKSIZE = true;
	
	protected int breakAfterCount;
	protected int minimumLinesPerBlock;
	
	private AggregatingHashtable linePositionsMap;
	private ArrayList linesInOriginalOrder;
	private ArrayList blockedList;
	
	/** Hidden do-nothing constructor for derivations. */
	protected Concordance()	{
	}

	/** Start a concordance search with a list of objects, without a ValidityFilter. */
	public Concordance(List lines)	{
		this(lines, null);
	}
	
	/** Start a concordance search with a list of objects and a ValidityFilter. */
	public Concordance(List lines, ValidityFilter filter)	{
		this(lines, filter, 0, 0);
	}
	
	/** Start a concordance search with a list of objects and a ValidityFilter. */
	public Concordance(List lines, ValidityFilter filter, int breakAfterCount, int minimumLinesPerBlock)	{
		this.breakAfterCount = breakAfterCount;
		this.minimumLinesPerBlock = minimumLinesPerBlock;
		startSearch(lines, filter);
	}
	
	/**
		Searches for concordances and provides lists in member variables for result views.
		@param lines list of objects to check for concordances, e.g. text lines.
		@param filter implementer of interface ValidityFilter that can influence the search.
	*/
	protected void startSearch(List lines, ValidityFilter filter)	{
		startSearch(lines, filter, null);
	}
	
	protected void startSearch(List lines, ValidityFilter filter, CancelProgressObserver observer)	{
		loopList(lines, filter, observer);
	}
	
	
	private static class PositionWrapperList extends ArrayList
	{
		private boolean significant;
		
		boolean isSignificant()	{
			return significant;
		}
		void setSignificant(boolean significant)	{
			this.significant = significant;
		}
	}
	
	
	private void loopList(List lines, ValidityFilter filter, CancelProgressObserver observer)	{
		// loop over input lines and find multiple occurences of lines,
		// aggregate them in a Map, mark them when they are significant for blocking
		
		linePositionsMap = new AggregatingHashtable()	{
			/** Overridden to create a List that additionally holds the 'significant' flag (estimated by filter). */
			protected List createAggregationList()	{
				return new PositionWrapperList();
			}
		};
		
		linesInOriginalOrder = new ArrayList(lines.size());
		
		for (int i = 0; i < lines.size(); i++)	{
			if (observer != null && observer.canceled())
				return;
				
			Object line = lines.get(i);	// a LineWrapper in the case of text processing
			
			// make a hashing and check if line is significant for follower seeking
			Object key = null;
			boolean significant = true;	// by default all lines are significant
			if (filter != null)	{	// if there is a filter, use it for creating a key for that line
				key = filter.isValid(line);
				significant = (key != null);	// significant means the line was not filtered out
			}
			
			if (key == null)	// need a key anyway
				key = line.toString();

			boolean isNew = (linePositionsMap.get(key) == null);
			if (isNew)
				linesInOriginalOrder.add(key);	// store the key unique in order how lines occured
			
			// store value of key and and aggregate every following duplicate
			linePositionsMap.put(key, new PositionWrapper(i, createWrapper(line, i)));
			// all aggregated lines for a key have the same significance setting
			if (isNew)
				((PositionWrapperList) linePositionsMap.get(key)).setSignificant(significant);
		}
	}


	/** Implements a factory method. Override to wrap the passed object. This implementation just returns object. */
	protected Object createWrapper(Object object, int index)	{
		return object;
	}

	/**
		Returns a List of Blocks that contains the mulitple occuring lines.
		This gives a tightened view of repeating (sequences of) lines.
	*/
	public List getBlockedResult()	{
		return getBlockedResult(null);
	}
	
	/** The same as above, with an observer. */
	public List getBlockedResult(CancelProgressObserver observer)	{
		if (blockedList != null)
			return blockedList;
		
		blockedList = new ArrayList(linesInOriginalOrder.size());
		
		// loop over unique but originally ordered line list
		for (int i = 0; i < linesInOriginalOrder.size(); i++)	{
			if (observer != null && observer.canceled() ||
					breakAfterCount > 0 && breakAfterCount <= blockedList.size())
				return blockedList;
			
			PositionWrapperList positions = (PositionWrapperList) linePositionsMap.get(linesInOriginalOrder.get(i));	// get list of position wrappers
			ArrayList positionsToFollow = getSufficientPositionsToFollow(positions);	// filter out those that are not significant, or are already in a block
			
			if (positionsToFollow != null)	{
				// look for lines that can be blocked with this one
				Block block = tryToExpandToBlock(positionsToFollow, observer);
				if (block == null)	// no followers found, report a one-line block
					block = new Block(positionsToFollow);

				if (isBlockSufficient(block.getPartCount()))	{
					blockedList.add(block);
					i--;	// do this pass again as there might be indexes not followed yet
				}
				else	{
					block.dismiss();	// reset alreadyInABlock flags
				}
			}
		}
		return blockedList;
	}

	// measures the line ("part") count of one occurence in a Block (all occurences must have the same count)
	private boolean isBlockSufficient(int numberOfLines)	{
		 return minimumLinesPerBlock <= 0 || numberOfLines >= minimumLinesPerBlock;
	}
	
	// measures the occurences count of a possible follower list
	private boolean isFollowersCountSufficient(int numberOfOccurences)	{
		 return numberOfOccurences > 1;
	}
	
	// MUST NOT return the original List, always return a clone, or null
	private ArrayList getSufficientPositionsToFollow(PositionWrapperList positions)	{
		if (positions.isSignificant() == false)
			return null;	// has been filtered out
		
		int size = positions.size();
		if (isFollowersCountSufficient(size) == false)
			return null;	// is not occuring more than once
		
		ArrayList toFollow = new ArrayList(size);
		for (int i = 0; i < size; i++)	{
			PositionWrapper position = (PositionWrapper) positions.get(i);
			if (position.isAlreadyInABlock() == false)
				toFollow.add(position);
		}
		return isFollowersCountSufficient(toFollow.size()) ? toFollow : null;
	}
	
	private Block tryToExpandToBlock(ArrayList positionsToFollow, CancelProgressObserver observer)	{
		Block block = null;
		final int NEEDED_POSITIONS_UNDEF = -2;
		int neededPositions = NEEDED_POSITIONS_UNDEF;
		
		for (int linesIndex = 0; linesIndex < linesInOriginalOrder.size(); linesIndex++)	{
			if (observer != null && observer.canceled())
				return block;
			
			// as the match method removes from list, we need a clone in the case it was unsuccessful
			List positionsToFollowClone = (List) positionsToFollow.clone();
			List positionsToCheck = (List) linePositionsMap.get(linesInOriginalOrder.get(linesIndex));
			ArrayList foundFollowers = matchFollowerPositions(positionsToFollowClone, positionsToCheck);
			
			// when successful, positionsToFollowClone was shrinked to those who had been matched
			int foundFollowersSize = (foundFollowers != null) ? foundFollowers.size() : NEEDED_POSITIONS_UNDEF + 1;
			
			// set the needed size when not already set and having more than one follower
			if (neededPositions == NEEDED_POSITIONS_UNDEF && isFollowersCountSufficient(foundFollowersSize))	{
				neededPositions = foundFollowersSize;
				block = new Block(positionsToFollowClone);	// the alreadyInABlock flags get set to true
			}
			else	// found no more match, or the number of matches differs from that before
			if (neededPositions != foundFollowersSize)	{
				// Maximizing the size (part count) of found blocks requires backtracking and removing
				// those occurences from the block that did not have the found followers
				if (MAXIMIZE_BLOCKSIZE && isFollowersCountSufficient(foundFollowersSize) && foundFollowersSize < neededPositions)	{
					block.removeOccurencesNotContinuedBy(foundFollowers);
					neededPositions = foundFollowersSize;
				}
				else	{	// ommit this pass
					continue;	// nothing found
				}
			}
			
			block.addPart(foundFollowers);
			
			positionsToFollow = foundFollowers;	// shift to foundFollowers
			linesIndex = -1;	// found followers, search for their followers by restarting the loop
		}
		return block;
	}
	
	// MUST NOT change positionsToCheck because this is an original List from Map
	private ArrayList matchFollowerPositions(List positionsToFollow, List positionsToCheck)	{
		ArrayList list = null;
		
		for (int i = 0; i < positionsToFollow.size(); i++)	{	// do NOT change order of this loop!
			PositionWrapper thisPosition = (PositionWrapper) positionsToFollow.get(i);
			
			boolean found = false;
			for (int j = 0; found == false && j < positionsToCheck.size(); j++)	{
				PositionWrapper followerPosition = (PositionWrapper) positionsToCheck.get(j);
				if (followerPosition.isAlreadyInABlock() == false && isFollower(thisPosition, followerPosition))	{
					if (list == null)
						list = new ArrayList(positionsToFollow.size());
					list.add(followerPosition);
					found = true;
				}
			}
			
			if (found == false)	{
				positionsToFollow.remove(i);
				i--;	// must do this because of remove()
			}
		}
		return list;
	}

	private static boolean isFollower(PositionWrapper thisPosition, PositionWrapper followerPosition)	{
		return thisPosition.position + 1 == followerPosition.position;
	}

	
	/**
		Wraps a position (within master list of objects)
		and the detected object (not its key used for hashing).
		This is Comparable for sorting by position (sorted view).
	*/
	private static class PositionWrapper implements Comparable
	{
		public final int position;
		public final Object object;
		private boolean alreadyInABlock;
		
		PositionWrapper(int position, Object object)	{
			this.position = position;
			this.object = object;
		}
		
		public int compareTo(Object o)	{
			PositionWrapper pw = (PositionWrapper)o;
			return position < pw.position ? -1 : position == pw.position ? 0 : 1;
		}
	
		void setAlreadyInABlock(boolean alreadyInABlock)	{
			this.alreadyInABlock = alreadyInABlock;
		}
		boolean isAlreadyInABlock()	{
			return alreadyInABlock;
		}
		
		public String toString()	{
			return position+": "+object; 
		}
	}


	/**
		Encapsulates m objects ("parts", lines) that occured n times (occurences)
		immediately after each other in some list of data (e.g. a text line list).
		Each object has a position where it lived in the original list,
		each of those positions is accessed by occurence and "part" index.
		
		The block "ab,ab,ab" would have occurences count = 3 and part count = 2.
	*/
	public static class Block
	{
		private List occurencesList = new ArrayList();	// contains part lists
		
		Block(List firstPartPositions)	{
			addPart(firstPartPositions, true);
		}
		
		void addPart(List partPositions)	{
			addPart(partPositions, false);
		}
		
		// add another found block part in form of a List of PositionWrappers
		private void addPart(List partPositions, boolean firstCall)	{
			if (firstCall == false && getOccurencesCount() != partPositions.size())
				throw new IllegalArgumentException("Part (occurences="+partPositions.size()+") must have same occurences count as others: "+getOccurencesCount());
			
			// add every part position to the end of the part lists in occurences list
			for (int i = 0; i < partPositions.size(); i++)	{
				PositionWrapper position = (PositionWrapper) partPositions.get(i);
				if (position.isAlreadyInABlock())
					throw new IllegalArgumentException("Can not add a position that is already in another block: "+position);
				
				List sequence;
				if (firstCall)	{	// adding first block
					sequence = new ArrayList();	// do not know how long this list will become
					occurencesList.add(sequence);
				}
				else	{	// adding further parts
					sequence = (List) occurencesList.get(i);
				}
				
				sequence.add(position);
				position.setAlreadyInABlock(true);
			}
		}
	
		// This remove is needed when backtracking on maximizing length of block part.
		// It removes all occurences that do not fit into sequence with the new ones.
		// The occurence count will decrease, and part count will stay constant.
		void removeOccurencesNotContinuedBy(List newPositions)	{
			// loop from tail to remove correctly
			for (int occurenceIndex = getOccurencesCount() - 1; occurenceIndex >= 0; occurenceIndex--)	{
				// read the occurence
				List sequence = (List) occurencesList.get(occurenceIndex);
				// read last position of the occurence
				PositionWrapper tailPosition = (PositionWrapper) sequence.get(sequence.size() - 1);
			
				// the last must have a follower among the newPositions
				boolean doRemoveThis = true;
				for (int newPositionsIndex = 0; doRemoveThis == true && newPositionsIndex < newPositions.size();  newPositionsIndex++)	{
					PositionWrapper newPosition = (PositionWrapper) newPositions.get(newPositionsIndex);
					if (isFollower(tailPosition, newPosition))
						doRemoveThis = false;
				}
			
				if (doRemoveThis)	{
					occurencesList.remove(occurenceIndex);	// remove this occurence, as it won't be continued
					
					for (int i = 0; i < sequence.size(); i++)	// reset all alreadyInABlock flags of the removed occurence
						((PositionWrapper) sequence.get(i)).setAlreadyInABlock(false);
				}
			}
		}
		
		// when a block was not accepted because it was too small its alreadyInABlock flags must be reset
		void dismiss()	{
			for (int occurenceIndex = getOccurencesCount() - 1; occurenceIndex >= 0; occurenceIndex--)	{
				List sequence = (List) occurencesList.get(occurenceIndex);
				for (int i = 0; i < sequence.size(); i++)
					((PositionWrapper) sequence.get(i)).setAlreadyInABlock(false);
			}
			occurencesList = null;
		}
		
		/** Returns the number of occurences of the parts within this block. */
		public int getOccurencesCount()	{
			return occurencesList.size();
		}
		
		/** Returns the number of parts (e.g. lines) of first occurence within this block (all occurences have the same part count). */
		public int getPartCount()	{
			return ((List) occurencesList.get(0)).size();
		}
		
		/** Returns the part (e.g. text line) of passed occurence and part index. The index must be between 0 and part count - 1. */
		public Object getPartObject(int occurence, int part)	{
			List sequence = (List) occurencesList.get(occurence);
			PositionWrapper position = (PositionWrapper) sequence.get(part);
			return position.object;
		}
	}

}
