package fri.util.diff;

import java.util.Vector;

/**
	Creates a layouted text where matching lines appear on the same
	line in both BalancedLines buffers. Different lines will be between,
	where the peer has empty lines.
	<p>
	Usage:
	<pre>
		BalancedLines [] lines = BalancedLines.createBalancedLines(lines1, lines2);
	</pre>
*/

public class BalancedLines extends Vector
{
	public static BalancedLines [] createBalancedLines(Object [] lines1, Object [] lines2)	{
		Diff d = new Diff(lines1, lines2);
		Diff.change script = d.diff_2(false);
		
		BalancedLines left = new BalancedLines(true, lines1, script);
		BalancedLines right = new BalancedLines(false, lines2, script);
		
		return new BalancedLines[] { left, right };
	}
	

	public BalancedLines(boolean isLeft, Object [] lines, Diff.change script)	{
		super(lines.length + lines.length / 2);
		
		for (int i = 0; i < lines.length; i++)	{
			Diff.change hunk;

			if ((hunk = isChangeStart(i, script, isLeft)) == null)	{
				add(createLine(i, lines[i], script, isLeft));
			}
			else	{
				// add blank lines to left or right if both have changes
				if (hunk.inserted > 0 && hunk.deleted > 0)	{
					add(new Line(i, lines[i], DiffChangeFlags.CHANGED));
					
					int delta = (hunk.inserted - hunk.deleted) * (isLeft ? 1 : -1);
					
					for (int j = 0; j < delta; j++)	{
						add(new Line(DiffChangeFlags.CHANGED));
					}
				}
				else	{
					int delta = 0;
					
					if (isLeft == true && hunk.inserted > 0 && hunk.deleted <= 0)	{
						delta = hunk.inserted;	// number of blank lines if insertion
					}
					else
					if (isLeft == false && hunk.inserted <= 0 && hunk.deleted > 0)	{
						delta = hunk.deleted;	// number of blank lines if deletion
					}

					for (int j = 0; j < delta; j++)	{	// add blank lines
						add(new Line());
					}

					add(createLine(i, lines[i], script, isLeft));
				}
			}
		}
	}
	
	private Diff.change isChangeStart(int line, Diff.change script, boolean isLeft)	{
		for (; script != null; script = script.link)	{
			int i = isLeft ? script.line0 : script.line1;
			if (i == line)	{
				return script;
			}
		}
		return null;
	}
	
	private String isInChangedRange(int line, Diff.change script, boolean isLeft)	{
		for (; script != null; script = script.link)	{
			int start = isLeft ? script.line0 : script.line1;
			int end   = start + (isLeft == false ? script.inserted : script.deleted);
			if (line >= start && line < end)	{
				return makeChangeFlag(script, isLeft);
			}
		}
		return null;
	}

	private String makeChangeFlag(Diff.change hunk, boolean isLeft)	{
		if (hunk.inserted > 0 && hunk.deleted > 0)
			return DiffChangeFlags.CHANGED;
		if (isLeft == true && hunk.deleted > 0)
			return DiffChangeFlags.DELETED;
		if (isLeft == false && hunk.inserted > 0)
			return DiffChangeFlags.INSERTED;
		return null;
	}
	
	private Line createLine(int lineNumber, Object line, Diff.change script, boolean isLeft)	{
		String flag;
		if ((flag = isInChangedRange(lineNumber, script, isLeft)) != null)	{
			return new Line(lineNumber, line, flag);
		}
		return new Line(lineNumber, line);
	}
	
	
	
	
	public static class Line
	{
		private int lineNumber = -1;
		private Object line = "\n";
		private String changeFlag = null;
		private boolean valid = false;	// false if this is just a fill line
		
		public Line()	{
		}
		
		public Line(String changeFlag)	{
			this.changeFlag = changeFlag;
		}
		
		public Line(int lineNumber, Object line)	{
			this.lineNumber = lineNumber;
			this.line = line;
			valid = true;
		}
		
		public Line(int lineNumber, Object line, String changeFlag)	{
			this(lineNumber, line);
			this.changeFlag = changeFlag;
			valid = true;
		}
		
		public Object getLine()	{
			return line;
		}
		
		public int getLineNumber()	{
			return lineNumber;
		}
		
		public void setChangeFlag(String flag)	{
			this.changeFlag = flag;
		}

		public String getChangeFlag()	{
			return changeFlag;
		}
		
		public boolean isValid()	{
			return valid;
		}
		
		public String toString()	{
			return (lineNumber >= 0 ? ""+lineNumber : " ")+" "+
				(changeFlag != null && (""+line).trim().length() > 0 ? changeFlag : " ")+"\t"+
				(line != null ? (""+line).trim() : "");
		}
	}


	/** Test Main
	public static void main(String[] argv) throws java.io.IOException {
		String[] a = { "eins", "zwei", "drei", "vier", "fuenf", "sechs", "sieben", "acht" };
		String[] b = { "null", "eins", "dreei", "vier", "vier", "vier", "fuenf", "sieben", "acht" };
		
		Diff d = new Diff(a, b);
		Diff.change script = d.diff_2(false);
		
		Diff.change next = script;
		while (next != null) {
			System.err.println(next);
			next = next.link;
		}
		
		BalancedLines left = new BalancedLines(true, a, script);
		BalancedLines right = new BalancedLines(false, b, script);
		
		StringBuffer sb1 = new StringBuffer();
		for (int i = 0; i < left.size(); i++)	{
			sb1.append(left.get(i).toString()+"\n");
		}
		StringBuffer sb2 = new StringBuffer();
		for (int i = 0; i < right.size(); i++)	{
			sb2.append(right.get(i).toString()+"\n");
		}
		
		java.awt.Frame f = new java.awt.Frame("Diff");
		f.setLayout(new java.awt.GridLayout(1, 2));
		java.awt.TextArea ta1 = new java.awt.TextArea(sb1.toString());
		java.awt.TextArea ta2 = new java.awt.TextArea(sb2.toString());
		f.add(ta1);
		f.add(ta2);
		f.pack();
		f.show();
	}
*/
}