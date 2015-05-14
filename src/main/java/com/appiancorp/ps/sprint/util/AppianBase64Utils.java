package com.appiancorp.ps.sprint.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.exceptions.InvalidVersionException;
import com.appiancorp.suiteapi.common.exceptions.PrivilegeException;
import com.appiancorp.suiteapi.common.exceptions.StorageLimitException;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentOutputStream;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.DuplicateUuidException;
import com.appiancorp.suiteapi.content.exceptions.InsufficientNameUniquenessException;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.knowledge.Document;

public class AppianBase64Utils 
{
	
	public static Document decodeBase64ToDocument(String base64String, String filename, String extension, Long folder, ContentService cs) 
			throws PrivilegeException, InvalidContentException, InvalidVersionException, StorageLimitException, InsufficientNameUniquenessException, DuplicateUuidException, IOException
	{			
		Document newDocument = new Document(folder, filename, extension);

		ContentOutputStream outStr = cs.upload(newDocument,  ContentConstants.UNIQUE_NONE);

		byte[] decodedBytes = Base64.decodeBase64(base64String);

		outStr.write(decodedBytes);
		outStr.flush();
		outStr.close();

		return newDocument;				
	}

	public static String encodeDocumentToBase64(Document document) throws IOException
	{
		String localPath  = document.getInternalFilename();
		File file = new File(localPath);
		byte[] plainBytes = loadFile(file);		
		
		return Base64.encodeBase64String(plainBytes);
	}

	private static byte[] loadFile(File file) throws IOException 
	{
		InputStream inStream = new FileInputStream(file);

		long length = file.length();
		if (length > Integer.MAX_VALUE) 
		{
			throw new IOException("File is too large "+file.getName());
		}

		byte[] bytes = new byte[(int)length];

		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead=inStream.read(bytes, offset, bytes.length-offset)) >= 0) 
		{
			offset += numRead;
		}

		if (offset < bytes.length) 
		{
			throw new IOException("Could not completely read file "+file.getName());
		}

		inStream.close();
		return bytes;
	}
}
