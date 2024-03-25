package fri.gui.awt.image;

// Transparency handling and variable bit size courtesy of Jack Palevich.
// Copyright (C)1996,1998 by Jef Poskanzer <jef@acme.com>. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.

import java.util.*;
import java.io.*;
import java.awt.Image;
import java.awt.image.*;

public class GifEncoder extends ImageEncoder
{
	private boolean interlace = false;

	public GifEncoder(Image img, OutputStream out) throws IOException {
		super(img, out);
	}

	public GifEncoder(Image img, OutputStream out, boolean interlace) throws IOException {
		super(img, out);
		this.interlace = interlace;
	}

	public GifEncoder(ImageProducer prod, OutputStream out) throws IOException {
		super(prod, out);
	}

	public GifEncoder(ImageProducer prod, OutputStream out, boolean interlace) throws IOException {
		super(prod, out);
		this.interlace = interlace;
	}

	int width, height;
	int[][] rgbPixels;

	void encodeStart(int width, int height) throws IOException {
		this.width = width;
		this.height = height;
		rgbPixels = new int[height][width];
	}

	void encodePixels(int x, int y, int w, int h, int[] rgbPixels, int off, int scansize) throws IOException {
		for (int row = 0; row < h; ++row)
			System.arraycopy(rgbPixels, row * scansize + off, this.rgbPixels[y + row], x, w);

	}

	IntHashtable colorHash;

	void encodeDone() throws IOException {
		int transparentIndex = -1;
		int transparentRgb = -1;
		// Put all the pixels into a hash table.
		colorHash = new IntHashtable();
		int index = 0;
		for (int row = 0; row < height; ++row) {
			for (int col = 0; col < width; ++col) {
				int rgb = rgbPixels[row][col];
				boolean isTransparent = ((rgb >>> 24) < 0x80);
				if (isTransparent) {
					if (transparentIndex < 0) {
						transparentIndex = index;
						transparentRgb = rgb;
					} else if (rgb != transparentRgb) {
						rgbPixels[row][col] = rgb = transparentRgb;
					}
				}
				GifEncoderHashitem item = (GifEncoderHashitem) colorHash.get(rgb);
				if (item == null) {
					if (index >= 256)
						throw new IOException("too many colors for a GIF");
					item = new GifEncoderHashitem(rgb, 1, index, isTransparent);
					++index;
					colorHash.put(rgb, item);
				} else
					++item.count;
			}
		}

		int logColors;
		if (index <= 2)
			logColors = 1;
		else if (index <= 4)
			logColors = 2;
		else if (index <= 16)
			logColors = 4;
		else
			logColors = 8;

		int mapSize = 1 << logColors;
		byte[] reds = new byte[mapSize];
		byte[] grns = new byte[mapSize];
		byte[] blus = new byte[mapSize];
		for (Enumeration e = colorHash.elements(); e.hasMoreElements();) {
			GifEncoderHashitem item = (GifEncoderHashitem) e.nextElement();
			reds[item.index] = (byte) ((item.rgb >> 16) & 0xff);
			grns[item.index] = (byte) ((item.rgb >> 8) & 0xff);
			blus[item.index] = (byte) (item.rgb & 0xff);
		}

		GIFEncode(out, width, height, interlace, (byte) 0, transparentIndex, logColors, reds, grns, blus);
	}

	byte GetPixel(int x, int y) throws IOException {
		GifEncoderHashitem item = (GifEncoderHashitem) colorHash.get(rgbPixels[y][x]);
		if (item == null)
			throw new IOException("color not found");
		return (byte) item.index;
	}

	static void writeString(OutputStream out, String str) throws IOException {
		byte[] buf = str.getBytes();
		out.write(buf);
	}

	int Width, Height;
	boolean Interlace;
	int curx, cury;
	int CountDown;
	int Pass = 0;

	void GIFEncode(OutputStream outs, int Width, int Height, boolean Interlace, byte Background, int Transparent, int BitsPerPixel, byte[] Red, byte[] Green, byte[] Blue) throws IOException {
		byte B;
		int LeftOfs, TopOfs;
		int ColorMapSize;
		int InitCodeSize;
		int i;

		this.Width = Width;
		this.Height = Height;
		this.Interlace = Interlace;
		ColorMapSize = 1 << BitsPerPixel;
		LeftOfs = TopOfs = 0;

		CountDown = Width * Height;

		Pass = 0;

		if (BitsPerPixel <= 1)
			InitCodeSize = 2;
		else
			InitCodeSize = BitsPerPixel;

		curx = 0;
		cury = 0;

		writeString(outs, "GIF89a");

		Putword(Width, outs);
		Putword(Height, outs);

		B = (byte) 0x80; // Yes, there is a color map
		B |= (byte) ((8 - 1) << 4);
		B |= (byte) ((BitsPerPixel - 1));

		Putbyte(B, outs);

		Putbyte(Background, outs);

		Putbyte((byte) 0, outs);

		for (i = 0; i < ColorMapSize; ++i) {
			Putbyte(Red[i], outs);
			Putbyte(Green[i], outs);
			Putbyte(Blue[i], outs);
		}

		if (Transparent != -1) {
			Putbyte((byte) '!', outs);
			Putbyte((byte) 0xf9, outs);
			Putbyte((byte) 4, outs);
			Putbyte((byte) 1, outs);
			Putbyte((byte) 0, outs);
			Putbyte((byte) 0, outs);
			Putbyte((byte) Transparent, outs);
			Putbyte((byte) 0, outs);
		}

		Putbyte((byte) ',', outs);

		Putword(LeftOfs, outs);
		Putword(TopOfs, outs);
		Putword(Width, outs);
		Putword(Height, outs);

		if (Interlace)
			Putbyte((byte) 0x40, outs);
		else
			Putbyte((byte) 0x00, outs);

		Putbyte((byte) InitCodeSize, outs);

		compress(InitCodeSize + 1, outs);

		Putbyte((byte) 0, outs);

		Putbyte((byte) ';', outs);
	}

	void BumpPixel() {
		++curx;

		if (curx == Width) {
			curx = 0;

			if (!Interlace)
				++cury;
			else {
				switch (Pass) {
					case 0 :
						cury += 8;
						if (cury >= Height) {
							++Pass;
							cury = 4;
						}
						break;

					case 1 :
						cury += 8;
						if (cury >= Height) {
							++Pass;
							cury = 2;
						}
						break;

					case 2 :
						cury += 4;
						if (cury >= Height) {
							++Pass;
							cury = 1;
						}
						break;

					case 3 :
						cury += 2;
						break;
				}
			}
		}
	}

	static final int EOF = -1;

	int GIFNextPixel() throws IOException {
		byte r;

		if (CountDown == 0)
			return EOF;

		--CountDown;

		r = GetPixel(curx, cury);

		BumpPixel();

		return r & 0xff;
	}

	void Putword(int w, OutputStream outs) throws IOException {
		Putbyte((byte) (w & 0xff), outs);
		Putbyte((byte) ((w >> 8) & 0xff), outs);
	}

	void Putbyte(byte b, OutputStream outs) throws IOException {
		outs.write(b);
	}

	static final int BITS = 12;

	static final int HSIZE = 5003; // 80% occupancy

	int n_bits; // number of bits/code
	int maxbits = BITS; // user settable max # bits/code
	int maxcode; // maximum code, given n_bits
	int maxmaxcode = 1 << BITS; // should NEVER generate this code

	final int MAXCODE(int n_bits) {
		return (1 << n_bits) - 1;
	}

	int[] htab = new int[HSIZE];
	int[] codetab = new int[HSIZE];

	int hsize = HSIZE; // for dynamic table sizing

	int free_ent = 0; // first unused entry

	boolean clear_flg = false;

	int g_init_bits;

	int ClearCode;
	int EOFCode;

	void compress(int init_bits, OutputStream outs) throws IOException {
		int fcode;
		int i /* = 0 */;
		int c;
		int ent;
		int disp;
		int hsize_reg;
		int hshift;

		g_init_bits = init_bits;

		// Set up the necessary values
		clear_flg = false;
		n_bits = g_init_bits;
		maxcode = MAXCODE(n_bits);

		ClearCode = 1 << (init_bits - 1);
		EOFCode = ClearCode + 1;
		free_ent = ClearCode + 2;

		char_init();

		ent = GIFNextPixel();

		hshift = 0;
		for (fcode = hsize; fcode < 65536; fcode *= 2)
			++hshift;
		hshift = 8 - hshift;

		hsize_reg = hsize;
		cl_hash(hsize_reg); // clear hash table

		output(ClearCode, outs);

		outer_loop : while ((c = GIFNextPixel()) != EOF) {
			fcode = (c << maxbits) + ent;
			i = (c << hshift) ^ ent; // xor hashing

			if (htab[i] == fcode) {
				ent = codetab[i];
				continue;
			} else if (htab[i] >= 0) // non-empty slot
				{
				disp = hsize_reg - i; // secondary hash (after G. Knott)
				if (i == 0)
					disp = 1;
				do {
					if ((i -= disp) < 0)
						i += hsize_reg;

					if (htab[i] == fcode) {
						ent = codetab[i];
						continue outer_loop;
					}
				} while (htab[i] >= 0);
			}
			output(ent, outs);
			ent = c;
			if (free_ent < maxmaxcode) {
				codetab[i] = free_ent++; // code -> hashtable
				htab[i] = fcode;
			} else
				cl_block(outs);
		}
		output(ent, outs);
		output(EOFCode, outs);
	}

	int cur_accum = 0;
	int cur_bits = 0;

	int masks[] = { 0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F, 0x00FF, 0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF };

	void output(int code, OutputStream outs) throws IOException {
		cur_accum &= masks[cur_bits];

		if (cur_bits > 0)
			cur_accum |= (code << cur_bits);
		else
			cur_accum = code;

		cur_bits += n_bits;

		while (cur_bits >= 8) {
			char_out((byte) (cur_accum & 0xff), outs);
			cur_accum >>= 8;
			cur_bits -= 8;
		}

		if (free_ent > maxcode || clear_flg) {
			if (clear_flg) {
				maxcode = MAXCODE(n_bits = g_init_bits);
				clear_flg = false;
			} else {
				++n_bits;
				if (n_bits == maxbits)
					maxcode = maxmaxcode;
				else
					maxcode = MAXCODE(n_bits);
			}
		}

		if (code == EOFCode) {
			while (cur_bits > 0) {
				char_out((byte) (cur_accum & 0xff), outs);
				cur_accum >>= 8;
				cur_bits -= 8;
			}

			flush_char(outs);
		}
	}

	void cl_block(OutputStream outs) throws IOException {
		cl_hash(hsize);
		free_ent = ClearCode + 2;
		clear_flg = true;

		output(ClearCode, outs);
	}

	void cl_hash(int hsize) {
		for (int i = 0; i < hsize; ++i)
			htab[i] = -1;
	}

	int a_count;

	void char_init() {
		a_count = 0;
	}

	byte[] accum = new byte[256];

	void char_out(byte c, OutputStream outs) throws IOException {
		accum[a_count++] = c;
		if (a_count >= 254)
			flush_char(outs);
	}

	void flush_char(OutputStream outs) throws IOException {
		if (a_count > 0) {
			outs.write(a_count);
			outs.write(accum, 0, a_count);
			a_count = 0;
		}
	}

}

class GifEncoderHashitem {

	public int rgb;
	public int count;
	public int index;
	public boolean isTransparent;

	public GifEncoderHashitem(int rgb, int count, int index, boolean isTransparent) {
		this.rgb = rgb;
		this.count = count;
		this.index = index;
		this.isTransparent = isTransparent;
	}

}