
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;


/*******************************************************************************
	ZipArchive.java
		省容量化、エラー無視、入力をInputstreamに変更
		7-zipで圧縮時に2748byte

	このソースコードは
	CPL(Common Public License) Version 1.0で配布されています。

	subsequent contribution by soft.spdv.net
							mail:soft@spdv.net

	Usage
		Inputstream in = new ZipArchive(in);
		len = in.read(bytearray);

 *******************************************************************************/


/**
 * ZipArchive.java
 *
 * Created: Mon Apr 14 10:49:56 2003
 * $Id: ZipArchive.java,v 1.11 2003/06/19 01:11:22 gama Exp $
 *
 * @author <a href="mailto:gama@lifemedia.co.jp">Yasuhiro Magara</a>
 */


/**
 * Read a zip archive and return entries, uncompressed size and inflated data stream.
 * <p>
 * This class has two functions.
 * One is to act as a zip archive which includes some file entries, 
 * ant the other is to act as an inflater stream of an entry in a zip archive.
 * Normally these two functions should be implemented as two discrete classes,
 * but I merged them because of size reason.
 * Multiple classes needs large jar size than single class.
 * This class is intended to use on J2ME CLDC
 * and size issue is more importand than the separation of functions.
 * </p>
 * <p>
 * このクラスには2つの機能があります。
 * ひとつは、複数のエントリを含んだzipアーカイブとして振舞う機能、
 * もうひとつは、zipアーカイブ中のあるエントリを伸張するInputStreamとしての機能です。
 * 普通ならば、これらの二つの機能は別々のクラスに実装されるべきものですが、
 * サイズ上の理由から単一のクラスに統合されています。
 * 複数のクラスは、単一のクラスより大きなjarファイルになります。
 * このクラスはJ2ME CLDCでの利用を想定しており、
 * 機能の分離以上にサイズの節減を重要視しています。
 * </p>
 * <p>
 * Example of usage is shwon below.
 * </p>
 * <pre>
 *	 ZipArchive za = new ZipArchive(buf);
 *	 String[] names = za.entries();
 *	 for (int e = 0; e &lt; names.length; e++) {
 *		 System.out.println("file name: " + names[e]
 *												+ ", uncompressed size: "
 *												+ za.getSize(names[e]));
 *		 InputStream is = za.getInputStream(names[e]);
 *		 int n = 0;
 *		 int l;
 *		 byte[] rb = new byte[0x1000];
 *		 while ((l = is.read(rb, 0, rb.length)) != -1) {
 *			 n += l;
 *		 }
 *		 is.close();
 *		 System.out.println("output size = " + n + '\n');
 *	 }
 *	 za.close();
 * </pre>
 * @version $Revision: 1.11 $
 */
public class ZipArchive extends InputStream {
//	private byte[] ab; // archive binary or archive entry buffer
	private InputStream in;	// inputstream
	private byte[] buf = new byte[1];					// buffer of inputstream

	///////////////////////////////////////////
	//////// Inflater InputStream part ////////
	///////////////////////////////////////////

	// extra bit of length code
	private static final int[] LX = {
		0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
		3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0
	};
	// extra bit of distance code
	private static final int[] DX = {
		0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
		7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13
	};

	// base length
	private static final int[] LB = {
		3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
		35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258};
	// base distance
	private static final int[] DB = {
		1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
		257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577
	};

	// length of the bit length codes order
	private static final byte[] bo = {
		16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15
	};

	private static int[] FL = new int[288]; // fixed Huffman literal code table
	private static int[] LN = new int[16 + 1];
	private static int[] FD = new int[32]; // fixed Huffman distance code table
	private static final int[] DN = {0, 0, 0, 0, 0, 32};
	// for dynamic Huffman deflated block
	private int[] DL; // Dynamic Huffman Code Table
	private int[] DD; // Dynamic Huffman Distance Table
	private int[] ml = new int[1];	// max/min bits of literal code
	private int[] nl;	// next code of literal
	private int[] md = new int[1];	// max/min bits of distance code
	private int[] nd;	// next code of distance code

//	private byte[] ab; // archive buffer
	private int pa;	// archive ptr to fetch
//	private int ae; // archive end point

	private int bb;	 // bit buffer
	private int pl;	 // prepared bit length

	private byte[] ib = new byte[0x8000]; // inflate buffer(32KB)
	private int fp = 0;	 // output buffer fill pointer
	private int rp = 0;	// output buffer consume pointer
	private boolean lb;	 // last block
	private boolean be;	 // block end
	private int bt;			 // block tpe
	private int se;	 // end of stored block
	private byte[] bf = new byte[1];

/**
 * Create instance as InputStream.
 * @param b buffer to read
 * @param off offset
 * @param l length
 */
/*	private ZipArchive(byte[] b, int o, int l) {
		ab = b;
		pa = o;
		ae = o + l;
		be = true;
		ib = new byte[0x8000];
		ml = new int[1];	// max/min bits of literal code
		md = new int[1];	// max/min bits of distance code
		bf = new byte[1]; // one byte read() buffer
	}
*/	
	ZipArchive(InputStream i) {
		in = i;

//		ab = b;
		pa = 0;
//		ae = o + l;
		be = true;
//		ib = new byte[0x8000];
//		ml = new int[1];	// max/min bits of literal code
//		md = new int[1];	// max/min bits of distance code
//		bf = new byte[1]; // one byte read() buffer
	}


	/**
	 * Read an inflated data from zip entry.
	 * <p>
	 * You should call this method via InputStream 
	 * returned by jp.imi.zjp.ZipArchive#getInputStream(String).
	 * </p>
	 * @see java.io.InputStream#read()
	 * @see #getInputStream(String)
	 */
	public int read() throws IOException {
		//int l = read(bf, 0, 1);
		//return (l == -1 ? -1 : (0xff & bf[0]));
//		return (read(bf, 0, 1) == -1 ? -1 : (0xff & bf[0]));
//	}

	//public int read(byte[] b) throws IOException {
	//	return read(b, 0, b.length);
	//}

	/**
	 * Read an inflated data from zip entry.
	 * <p>
	 * You should call this method via InputStream 
	 * returned by jp.imi.zjp.ZipArchive#getInputStream(String).
	 * </p>
	 * @see java.io.InputStream#read(byte[], int, int)
	 * @see #getInputStream(String)
	 */
//	public int read(byte[] b, int o, int len) throws IOException {
		//byte[] b = bf;
		//int o = 0;
		//int len = 1;

		//int btl = 1;//o + len;	// tail of read buffer
		int oc = 0; // output count
		int i;

		for (;;) {
			/*
			 * 0				 filled		 rptr		0x8000
			 * +-----------+---------+--------+
			 * 
			 * 0				 rptr		 filled		0x8000
			 * +-----------+---------+--------+
			 */
			if (rp < fp) {
				// have some buffered data
//				if (fp - rp > 0x8000) throw new IOException(/*"Bug!"*/); 
				int cpl = 0;
				if ((rp & 0x7fff) >= (fp & 0x7fff)) {
					cpl = 0x8000 - (rp & 0x7fff);
					if (cpl > /*btl*/1 - oc) cpl = /*btl*/1 - oc;
					System.arraycopy(ib, rp & 0x7fff, bf, oc, cpl);
					//o += cpl;
					rp += cpl;
					oc += cpl;
				}
				if (oc < 1/*btl*/) {
					cpl = (/*btl*/1 - oc) < (fp - rp) ? (/*btl*/1 - oc) : (fp - rp);
				} else {
					cpl = 0;
				}
				System.arraycopy(ib, rp & 0x7fff, bf, oc, cpl);
				//o += cpl;
				rp += cpl;
				oc += cpl;
			}
			if (oc == 1/*btl*/) break;

			// prepare to inflate block
			if (be) {
				// require new block to inflate
				if (lb) {
					// there is no block to inflate
					if (oc > 0) {
						// buffered is filled partly
						break;
					} else {
						// nothing to return. reached EOF.
						return -1;
					}
				}
				// clear block end flag
				be = false;
				// read block header when block end
				rb(1); // NEEDBITS
				lb = (bb & 1) != 0;
				bb >>>= 1;
				pl--; // DUMPBITS
				rb(2);
				bt = (3 & bb);
				bb >>>= 2;
				pl -= 2;
				if (bt == 0) {
					// Block is stored
					bb >>>= pl;
					pl = 0;

					in.read(buf);
					int cnt = (0xff & buf[0]);
					in.read(buf);
					cnt += ((0xff & buf[0]) << 8);
					
					in.read(buf);
					int cnt2 = (0xff & buf[0]);
					i = in.read(buf);
					cnt2 += ((0xff & buf[0]) << 8);

					if(i == -1){throw new EOFException(/*"End of archive."*/);}
					
					//int cnt = (0xff & ab[pa]) + ((0xff & ab[pa + 1]) << 8);
//					if (cnt != (0xffff & ~cnt2/*((0xff & ab[pa + 2]) + ((0xff & ab[pa + 3]) << 8))*/)) {
//						throw new IOException(/*"Illegal Stored block."*/);
//					}
					pa += 4;
					se = pa + cnt;
				} else if (bt == 2) {
					// Dynamic Huffman
					rb(5);
					int HL = (0x1f & bb) + 257;
					bb >>>= 5;
					pl -= 5;
					rb(5);
					int HD = (0x1f & bb) + 1;
					bb >>>= 5;
					pl -= 5;
					rb(4);
					int HC = (0xf & bb) + 4;
					bb >>>= 4;
					pl -= 4;

					int[] t = new int[19];
					for (i = 0; i < HC; i++) {
						rb(3);
						t[bo[i]] = (0x7 & bb);
						bb >>>= 3;
						pl -= 3;
					}
					int[] cd = new int[19];
					int[] tn = new int[9];
					int[] mm = new int[1]; // min,max
					h_b(t, 8, cd, tn, mm);

					// Dynamic Huffman Literal/Distance code length Table
					t = new int[HL + HD];

					int ll = 0; // last bit length (to repeat)
					for (i = 0; i < HL + HD;) {
						int nb = 0;
						int hc = 0;
						do {
							nb++;
							rb(1);
							hc <<= 1;
							hc |= (bb & 1);
							bb >>>= 1;
							pl--;
						} while (tn[nb] <= hc && nb < mm[0]);

						int c;
						for (c = 0; c < cd.length; c++) {
							if (cd[c] == hc)
								break;
						}
//						if (c == cd.length)
//							throw new IOException(/*"No code found."*/);
						if (c < 16) {
							t[i++] = ll = c;
						} else if (c == 16) {
							rb(2);
							int rpt = 3 + (3 & bb);
							bb >>>= 2;
							pl -= 2;
							while (rpt-- > 0)
								t[i++] = ll;
						} else if (c == 17) {
							rb(3);
							int rpt = 3 + (7 & bb);
							bb >>>= 3;
							pl -= 3;
							ll = 0;
							while (rpt-- > 0)
								t[i++] = 0;
						} else if (c == 18) {
							rb(7);
							int rpt = 11 + (0x7f & bb);
							bb >>>= 7;
							pl -= 7;
							ll = 0;
							while (rpt-- > 0)
								t[i++] = 0;
//						} else {
//							throw new IOException(/*"Bug!"*/);
						}
					}
					DL = new int[287]; // Dynamic Huffman Code Table
					nl = new int[17];
					int[] tt = new int[HL];
					System.arraycopy(t, 0, tt, 0, HL);
					h_b(tt, 16, DL, nl, ml);

					DD = new int[32]; // Dynamic Huffman Distance Table
					nd = new int[33];
					tt = new int[HD];
					// Dynamic Huffman Distance code length Table
					System.arraycopy(t, HL, tt, 0, HD);
					t = null;
					h_b(tt, 16, DD, nd, md);
					tt = null;
//				} else if (bt == 3) {
					// Error: Reserved.
//					throw new IOException(/*"Error in compressed data."*/);
				}
			}
			if (bt == 0) {
				// copy data to buffer
				int l = (se - pa) < (/*btl*/1 - oc) ? (se - pa) : (/*btl*/1 - oc);
				//System.arraycopy(ab, pa, b, o, l);
				/*int */i = 0;
				while(i < l){
					in.read(bf, oc + i, 1);
					i++;
				}
				pa += l;
				//o += l;
				oc += l;
			} else if (bt == 1) {
				// fill data to inflate buffer
				i_b(FL, FD, LN, DN, 16, 5);
			} else if (bt == 2) {
				// fill data to inflate buffer
				i_b(DL, DD, nl, nd, ml[0], md[0]);
//			} else {
//				throw new IOException(/*"Error in compressed data."*/);
			}
		}
//		return oc;
		return (oc == -1 ? -1 : (0xff & bf[0]));
	}
	
	/**
	 * Free some internal buffers.
	 * This should be called as both InputStream and ZipArchive.
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException  {
		in.close();
		in = null;
		/*ab = */ib = bf = null;
		DL = DD = ml = md = nd = null;
	}

	/**
	 * Get bits to bit buffer.
	 * @param n required bit length
	 */
	private void rb(int n) throws IOException {
		while (pl < n) {
			if(in.read(buf) == -1){throw new EOFException(/*"End of archive."*/);}
//			if (pa >= ae) {
//				throw new EOFException("End of archive.");
//			} 
			bb |= ((0xff & buf[0]/*ab[pa++]*/) << pl);
			pa++;
			pl += 8;
		}
	}

	/**
	 * Inflate block.
	 * @param lc literal code.
	 * @param dc distance code.
	 * @param l_n next code of literal code.
	 * @param d_n next code of distance code.
	 * @param lm max bit length of literal code.
	 * @param dm max bit length of distance code.
	 */
	private void i_b(
		int[] lc,
		int[] dc,
		int[] l_n,
		int[] d_n,
		int lm,
		int dm) throws IOException {
		while (fp - rp < 0x200) {
			int nb = 0;
			int hc = 0;
			do {
				nb++;
				rb(1);
				hc <<= 1;
				hc |= (bb & 1);
				bb >>>= 1; pl--;
			} while (l_n[nb] <= hc && nb < lm);

			int c;
			for (c = 0; c < lc.length; c++) {
				if (lc[c] == hc)
					break;
			}
			if (c < 256) {
				ib[(fp++) & 0x7fff] = (byte) (0xff & c);
			} else if (c == 256) {
				be = true;
				break; // end-of-block
			} else if (c >= 257 && c <= 285) {
				// decode length from extra bit
				c -= 257;
				int e = LX[c];	// extra bits
				rb(e);
				int cl = LB[c] + (((1<<e)-1) & bb);
				bb >>>= e; pl -= e;

				// decode distance code
				nb = 0;
				hc = 0;
				do {
					nb++;
					rb(1);
					hc <<= 1;
					hc |= (bb & 1);
					bb >>>= 1; pl--;
				} while (d_n[nb] <= hc && nb < dm);

				for (c = 0; c < dc.length; c++) {
					if (dc[c] == hc)
						break;
				}

				e = DX[c];
				rb(e);
				int d = DB[c] + (((1<<e) - 1) & bb);
				bb >>>= e; pl -= e;
				int tl = fp + cl;
				while (fp < tl) {
					ib[fp & 0x7fff] = ib[(fp - d) & 0x7fff];
					fp++;
				}
//			} else {
				// illegal code, c >= 286
//				throw new IOException(/*"Illegal code."*/);
			}
		}
	}

	/**
	 * Build Huffman codes.
	 * @param b array of code length for each code (in)
	 * @param mb maximum lookup bits (in)
	 * @param cd array of Huffman code (out)
	 * @param nc next code table (out) 
	 * @param mx maximum bit length (out)
	 */
	private static void h_b(
		int[] b,
		int mb,
		int[] cd,
		int[] nc,
		int[] mx) {
		// Count the number of codes for each code length.
		int[] bc = new int[mb + 1]; // bit length count table
		int p = b.length - 1; // counter, current code
		while (p >= 0) {
			bc[b[p]]++;
			p--;
		}

		// null input, do nothing
		if (bc[0] == b.length) return; 

		// 2)	Find the numerical value of the smallest code for each code length:
		int c = 0;
		bc[0] = 0;
		int bt;
		for (bt = 1; bt <= mb; bt++) {
			c = ((c + bc[bt - 1]) << 1);
			nc[bt] = c;
		}

		if (mx != null) {
			// Find maximum bit length
			for (bt = mb; bt > 0; bt--) {
				if (bc[bt] > 0)
					break;
			}
			mx[0] = bt; // maximum code length
		}

		// 3)	Assign numerical values to all codes
		int mc = b.length - 1;
		for (int n = 0; n <= mc; n++) {
			int len = b[n];
			if (len != 0) {
				cd[n] = nc[len];
				nc[len]++;
			} else {
				// never assign for length 0 => assign illegal value (-1)
				cd[n] = -1;
			}
		}
	}

	/**
	 * Build Fixed Huffman tables.
	 */
	static {
		int[] l = new int[288];
		int i = 0;
		for (; i < 144; i++) l[i] = 8;
		for (; i < 256; i++) l[i] = 9;
		for (; i < 280; i++) l[i] = 7;
		for (; i < 288; i++) l[i] = 8;
		h_b(l, 16, FL, LN, null);
		for (int j = 0; j < 32; j++) FD[j] = j;
	}
}// ZipArchive
