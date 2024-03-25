package fri.util.crypt;

import java.io.*;
import Acme.Crypto.*;

/**
	Encrypt or decrypt a text, given by bytes, with a key phrase, given as String.
	
	@author Fritz Ritzberger
*/

public class Crypt
{
	private Cipher algorithm;

	/**
		Pass parameters for the encryption/decryption, the key gets padded
		to the required count of bytes when necessary (fill with zeros).
	*/
	public Crypt(String key, String algorithm)	{
		this.algorithm = algorithm == null || algorithm.equals("IDEA")
			? new IdeaCipher(fill(key, 16))	// blocksize 8
			: algorithm.equals("DES3")
				? new Des3Cipher(fill(key, 16))	// blocksize 8
				: algorithm.equals("AES")
					? new AesCipher(fill(key, 16))	// blocksize 16
					: algorithm.equals("RC4")
						? new Rc4Cipher(key)
						: (Cipher)null;
	}
	
	public boolean hasValidAlgorithm()	{
		return algorithm != null;
	}
	
	/**
		Returns the decrypted/encrypted bytes from the text passed to the constructor.
		@param encrypt true for encryption, false for decryption
		@return de/encrypted bytes resulting from text passed to constructor
	*/
	public byte [] getBytes(byte [] data, boolean encrypt)
		throws IOException
	{
		if (algorithm == null)
			return data;
		
		if (encrypt)	{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			EncryptedOutputStream eout = (algorithm instanceof StreamCipher)
					? new EncryptedOutputStream((StreamCipher)algorithm, out)
					: new EncryptedOutputStream((BlockCipher)algorithm, out);
			eout.write(data);
			eout.flush(); eout.close();
			return out.toByteArray();
		}
		else	{	// decrypt
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			EncryptedInputStream ein = (algorithm instanceof StreamCipher)
					? new EncryptedInputStream((StreamCipher)algorithm, in)
					: new EncryptedInputStream((BlockCipher)algorithm, in);
			byte bytes[] = new byte[ein.available()];
			ein.read(bytes);
			ein.close();
			
			// remove trailing zeros
			int remainder = 0;
			for (int i = bytes.length - 1; i >= 0; i--)
				if (bytes[i] == 0)
					remainder++;
				else
					break;
					
			byte [] result = new byte[bytes.length - remainder];
			System.arraycopy(bytes, 0, result, 0, bytes.length - remainder);
			
			return result;
		}
	}

	private byte [] fill(String key, int targetSize)	{
		byte [] tgt = new byte[targetSize];
		byte [] src = key.getBytes();
		System.arraycopy(src, 0, tgt, 0, Math.min(src.length, targetSize));
		return tgt;
	}
	

	
	/** Test main. */
	public static void main(String [] args)
		throws IOException
	{
		String key = "5wasvrmhu9";
		Crypt crypt = new Crypt(key, "IDEA");
		byte [] bytes = crypt.getBytes(args[0].getBytes(), true);
		StringBuffer sb = new StringBuffer("byte [] bytes = new byte [] { ");
		for (int i = 0; i < bytes.length; i++)	{
			sb.append(bytes[i]);
			sb.append(", ");
		}
		sb.append(" };");
		System.out.println(sb);
	}

}
