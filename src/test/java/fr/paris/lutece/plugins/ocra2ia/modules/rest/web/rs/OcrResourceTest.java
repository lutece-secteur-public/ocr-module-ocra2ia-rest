package fr.paris.lutece.plugins.ocra2ia.modules.rest.web.rs;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;

import org.junit.Assert;
import org.junit.Test;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import net.sf.json.JSONObject;

public class OcrResourceTest { 

	@Test
	public void sendInSameTimeRibOCRRequestTest() throws IOException, InterruptedException {

		Map<String,String> filesResults= new HashMap<>();
		
		filesResults.put("RIB11 JPEG CORRECT (similaire RIB3).jpg", "{Rib result=30002_06900_0000032434G_03, Code Banque=30002, ClÃ© RIB=03, IBAN=FR3830002069000000032434G03, Account number=0000032434G, Code Guichet=06900, BIC=CRLYFRPP}");
		filesResults.put("RIB18 PHOTO CORRECT (similaire RIB1).jpg", "{Rib result=30004_00909_00001348585_17, Code Banque=30004, RIB Address=MR FRANCIS PACAUD OU MME GELINE PACAUD 25 AVENUE DIANE 94500 CHAMPIGNY SUR MARNE , ClÃ© RIB=17, IBAN=FR7630004009090000134858517, Account number=00001348585, Code Guichet=00909, BIC=BNPAFRPPXXX}");
		filesResults.put("RIB19 PHOTO CORRECT (similaire RIB2).jpg", "{Rib result=, Code Banque=, ClÃ© RIB=, IBAN=, Account number=, Code Guichet=, BIC=}");
		filesResults.put("RIB1 PDF CORRECT.pdf", "{Rib result=30004_00909_00001348585_17, Code Banque=30004, ClÃ© RIB=17, IBAN=FR7630004009090000134858517, Account number=00001348585, Code Guichet=00909, BIC=BNPAFRPPXXX}");
		filesResults.put("RIB10 TIFF CORRECT (similaire RIB1).tiff","{Rib result=30004_00909_00001348585_17, Code Banque=30004, ClÃ© RIB=17, IBAN=FR7630004009090000134858517, Account number=00001348585, Code Guichet=00909, BIC=BNPAFRPPXXX}");
		
		for (Entry<String,String> entry : filesResults.entrySet()) {
			Thread thread = new Thread() {
				public void run() {
					String fileName = entry.getKey();
					Map response = sendOcrRequest(fileName);
					String exepectedResult = entry.getValue();
					Assert.assertEquals(exepectedResult, response.toString());
					System.out.println(fileName+": "+response.toString());
				}
			};
			thread.start();
		}

		Thread.sleep(30000);
	}

	private Map sendOcrRequest(String fileName) {
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
		WebResource service = client.resource("http://localhost.paris.mdp:8080/site-ocr/rest/ocr/start");

		JSONObject my_data = new JSONObject();

		File file_upload = new File("src/test/resources/ribs/" + fileName);
		my_data.put("fileextension", file_upload.getName().split("\\.")[1]);
		my_data.put("documenttype", "rib");
		my_data.put("filecontent", convertFileToString(file_upload));

		ClientResponse client_response = service.accept(MediaType.APPLICATION_JSON)
				.header("content-type", MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.post(ClientResponse.class, my_data);

		Map response = client_response.getEntity(HashMap.class);

		client.destroy();

		return response;
	}

	public final String convertFileToString(File file) {
		byte[] bytes = null;
		try {
			bytes = Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String(Base64.getEncoder().encode(bytes));
	}

	// Convert a Base64 string and create a file
	public final void convertFile(String file_string, String file_name) throws IOException {
		byte[] bytes = Base64.getDecoder().decode(file_string);
		File file = new File("local_path/" + file_name);
		FileOutputStream fop = new FileOutputStream(file);
		fop.write(bytes);
		fop.flush();
		fop.close();
	}
}
