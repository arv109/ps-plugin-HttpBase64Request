package com.appiancorp.ps.sprint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.appiancorp.ps.sprint.util.AppianBase64Utils;
import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.common.exceptions.InvalidVersionException;
import com.appiancorp.suiteapi.common.exceptions.PrivilegeException;
import com.appiancorp.suiteapi.common.exceptions.StorageLimitException;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.DuplicateUuidException;
import com.appiancorp.suiteapi.content.exceptions.InsufficientNameUniquenessException;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.Unattended;
import com.appiancorp.suiteapi.process.palette.PaletteCategoryConstants;
import com.appiancorp.suiteapi.process.palette.PaletteInfo;
import com.appiancorp.suiteapi.type.Type;

@PaletteInfo(paletteCategory = PaletteCategoryConstants.INTEGRATION_SERVICES, palette = "Connectivity Services")
@Unattended

public class MakeBase64RequestToWS extends AppianSmartService 
{
	private static final Logger log = Logger.getLogger(MakeBase64RequestToWS.class);
	
	private static final String DOCID_TOKEN_PREFIX = "#";
	private static final String DOCID_TOKEN_SUFFIX = "#";
	private static final String FILE_EXTENSION_PDF = "pdf";
	
	//inputs
	private String requestString;
	private String fileName;
	private Long parentFolder;
	private String jsonPath;
	
	//outputs
	private String responseString;
	private Document document;
	private boolean isError;
	private String errorMessage;
	private int httpStatusCode;
	
	
	private final ContentService contentService;

	
	public MakeBase64RequestToWS(ContentService cs) 
	{
		super();
		this.contentService = cs;
	}

	@Override
	public void run() throws SmartServiceException 
	{
		try 
		{
			Document[] docsToConvert = getDocumentsInRequest(requestString);
	
			String newRequestString = createRequestWithEncodedDocuments(requestString, docsToConvert);
			
			callWebService(newRequestString);
			if(responseString != null)
				document = AppianBase64Utils.decodeBase64ToDocument(responseString, fileName, FILE_EXTENSION_PDF, parentFolder, contentService);
			else
				document = null;
		} 
		catch (IOException e) 
		{
			log.error(e.getMessage(), e);
		} 
		catch (PrivilegeException e) 
		{
			log.error(e.getMessage(), e);
		} 
		catch (InvalidContentException e)
		{
			log.error(e.getMessage(), e);
		} 
		catch (InvalidVersionException e) 
		{
			log.error(e.getMessage(), e);
		} 
		catch (StorageLimitException e)
		{
			log.error(e.getMessage(), e);
		}
		catch (InsufficientNameUniquenessException e) 
		{
			log.error(e.getMessage(), e);
		}
		catch (DuplicateUuidException e) 
		{
			log.error(e.getMessage(), e);
		}
				
	}
	
	
	private String createRequestWithEncodedDocuments(String requestStr, Document[] docsToConvert) throws IOException
	{
		for(int i = 0; i < docsToConvert.length; i++)
		{	
			Document thisDoc = docsToConvert[i];

			String replaceThis = DOCID_TOKEN_PREFIX +thisDoc.getId()+ DOCID_TOKEN_SUFFIX;			
			String withThis = AppianBase64Utils.encodeDocumentToBase64(thisDoc);
			
			requestStr = requestStr.replace(replaceThis, withThis);
		}
		
		return requestStr;
	}
		
	
	private Document[] getDocumentsInRequest(String stringToSearch)
	{		
		try 
		{
			ArrayList documentIds = new ArrayList<Long>();
			
			StringTokenizer tokenise = new StringTokenizer(stringToSearch, DOCID_TOKEN_PREFIX+DOCID_TOKEN_PREFIX); 

			while(tokenise.hasMoreTokens()) 
			{ 
				tokenise.nextToken(); 
				String val = "";
				if(tokenise.hasMoreTokens())
				{
					val = tokenise.nextToken(); 
					documentIds.add(new Long(val));			
				}	
			} 	
			
			Document[] knowledgeDocs = new Document[documentIds.size()];
			Long [] docIds = (Long[])documentIds.toArray(new Long[documentIds.size()]);
			
			for (int i = 0; i < docIds.length; i++)
			{
				knowledgeDocs[i] = (Document) contentService.download(docIds[i], ContentConstants.VERSION_CURRENT, false)[0];
			}
											
			return knowledgeDocs;
		}
		catch (PrivilegeException e) 
		{
			log.error(e.getMessage(), e);
			return null;
		}
		catch (InvalidContentException e) 
		{
			log.error(e.getMessage(), e);
			return null;
		}
		catch (InvalidVersionException e) 
		{
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	private void callWebService(String requestString)
	{ 
		try
		{
			URL url = new URL(jsonPath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "text/json");			
	 
			OutputStream os = conn.getOutputStream();
			os.write(requestString.getBytes());
			os.flush();
	 
			httpStatusCode = conn.getResponseCode();
			if (httpStatusCode != HttpURLConnection.HTTP_OK) 
			{
				isError = true;
				if(conn.getErrorStream() != null)
					errorMessage = conn.getErrorStream().toString();
			}
	 
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
	 
			
			log.error("Output from Server .... \n");
			while ((responseString = br.readLine()) != null) {
				responseString += responseString;
			}
	 
			log.error(responseString);	
			conn.disconnect();
		} 
		catch (MalformedURLException e) 
		{
			log.error(e.getMessage(), e);
		} 
		catch (ProtocolException e) 
		{
			log.error(e.getMessage(), e);
		}
		catch (IOException e) 
		{
			log.error(e.getMessage(), e);
		}
	}
	
	
	@Input(required = Required.ALWAYS)
	@Name("requestString")
	public void setRequestString(String val) {
		this.requestString = val;
	}

	@Input(required = Required.ALWAYS)
	@Name("fileName")
	public void setFileName(String val) {
		this.fileName = val;
	}

	@Input(required = Required.ALWAYS)
	@Name("parentFolder")
	@Type(namespace = Type.APPIAN_NAMESPACE, name="Folder")
	@FolderDataType
	public void setParentFolder(Long val) {
		this.parentFolder = val;
	}
	
	@Input(required = Required.ALWAYS)
	@Name("jsonPath")
	public void setJsonPath(String val) {
		this.jsonPath = val;
	}
	
	@Name("document")
	@Type(namespace = Type.APPIAN_NAMESPACE, name="Document")
	public Document getDocument() {
		return document;
	}

	@Name("isError")
	public boolean isError() {
		return isError;
	}

	@Name("errorMessage")
	public String getErrorMessage() {
		return errorMessage;
	}
	
	@Name("httpStatusCode")
	public int getHttpStatusCode() {
		return httpStatusCode;
	}
	
	@Name("responseString")
	public String getResponseString() {
		return responseString;
	}

}
