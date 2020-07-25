package es.aldago.dctm;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


public class RestOperations {

	public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException, ExecutionException {
		String pathCabinets = "http://192.168.94.130:8080/dctm-rest/repositories/dctm202/cabinets";		
		String pathObject = "http://192.168.94.130:8080/dctm-rest/repositories/dctm202/cabinets/0c00271080000107";
		String pathContent = "http://192.168.94.130:8080/dctm-rest/repositories/dctm202/objects/0c00271080000107/contents/content";
		String pathFolder = "http://192.168.94.130:8080/dctm-rest/repositories/dctm202/folders/0c00271080000107/folders";
		String pathFolderImport = "http://192.168.94.130:8080/dctm-rest/repositories/dctm202/folders/0c00271080000107/documents";
		String pathDocumentDelete = "http://192.168.94.130:8080/dctm-rest/repositories/dctm202/documents/090027108000415c";
		String pathFolderDelete = "http://192.168.94.130:8080/dctm-rest/repositories/dctm202/folders/0b00271080004127";
		
		HttpClient client = HttpClient.newHttpClient();
		String basicAuthStr = basicAuth("dmadmin", "dmadmin");

		System.out.println ("*****get Cabinet List *****");
		HttpRequest request = HttpRequest.newBuilder()
				.uri(new URI(pathCabinets))
				.GET()
				.header("Authorization", basicAuthStr)
				.build();

		client.sendAsync(request, BodyHandlers.ofString())
			    	.thenApply(HttpResponse::body)
			    	.thenAccept(System.out::println)
			    	.join();
				
				
		System.out.println ("*****get Object Properties *****");	
		request = HttpRequest.newBuilder()
				.uri(new URI(pathObject))
				.GET()
				.header("Authorization", basicAuthStr)
				.build();
				
		client.sendAsync(request, BodyHandlers.ofString())
			  	.thenApply(HttpResponse::body)
			  	.thenAccept(System.out::println)
			   	.join();
		
				
		System.out.println ("*****get Object Content *****");
		request = HttpRequest.newBuilder()
				.uri(new URI(pathContent))
				.GET()
				.header("Authorization", basicAuthStr)
				.build();
		
		CompletableFuture<String> response=client.sendAsync(request, BodyHandlers.ofString())
				.thenApply(HttpResponse::body);
				
		String content=response.get().toString();
		//GET Conent file URL in a very dirty way
		String fileURL=content.substring(content.indexOf("\"ACS\",\"href\":"));
		fileURL=fileURL.substring("\"ACS\",\"href\":\"".length(), fileURL.indexOf("\"},"));
		
		request = HttpRequest.newBuilder()
				.uri(new URI(fileURL))
				.GET()
				.header("Authorization", basicAuthStr)
				.build();
				
		client.send(request, BodyHandlers.ofFile(Paths.get("C:/Temp/pdf.pdf")));

		System.out.println ("*****create Folder on /Temp *****");
		String json = "{\"properties\" : {\"object_name\" : \"RESTFOLDER\", \"r_object_type\" :\"dm_folder\"}}";
		request = HttpRequest.newBuilder()
				.uri(new URI(pathFolder))
				.POST(BodyPublishers.ofString(json))
				.headers("Authorization", basicAuthStr,"content-type","application/vnd.emc.documentum+json")
				.build();
		
		client.sendAsync(request, BodyHandlers.ofString())
				.thenApply(HttpResponse::body)
				.thenAccept(System.out::println)
				.join();

		System.out.println ("*****create Documentum on /Temp *****");
		json = "{\"properties\" : {\"object_name\" : \"TESTDOCUMENT\", \"r_object_type\" :\"dm_document\"}}";  

		String boundary = new BigInteger(256, new Random()).toString();
		Map<Object, Object> data = new HashMap<>();
		data.put("metadata", json);
		data.put("content", Paths.get("d:/dctm202/Documentum_Server_20.2_Release_Notes.pdf"));
		
		request = HttpRequest.newBuilder()
				.uri(new URI(pathFolderImport))
				.POST(ofMimeMultipartData(data, boundary))
				.headers("Authorization", basicAuthStr,"content-type","multipart/form-data;boundary="+boundary,"Accept","application/vnd.emc.documentum+json")
				.build();

		client.sendAsync(request, BodyHandlers.ofString())
			.thenApply(HttpResponse::body)
			.thenAccept(System.out::println)
			.join();
	

		System.out.println ("*****Delete Folder/Document on /Temp *****");
		request = HttpRequest.newBuilder()
				.uri(new URI(pathFolderDelete))
				.DELETE()
				.header("Authorization", basicAuthStr)
				.build();
		
		client.sendAsync(request, BodyHandlers.ofString())
			.thenApply(HttpResponse::body)
			.thenAccept(System.out::println)
			.join();	
		
		request = HttpRequest.newBuilder()
				.uri(new URI(pathDocumentDelete))
				.DELETE()
				.header("Authorization", basicAuthStr)
				.build();
		
		client.sendAsync(request, BodyHandlers.ofString())
			.thenApply(HttpResponse::body)
			.thenAccept(System.out::println)
			.join();
		
	}
	
	private static String basicAuth(String username, String password) {
		return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
	}

	public static BodyPublisher ofMimeMultipartData(Map<Object, Object> data, String boundary) throws IOException {
		var byteArrays = new ArrayList<byte[]>();
		byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=").getBytes(StandardCharsets.UTF_8);
		
		for (Map.Entry<Object, Object> entry : data.entrySet()) {
			byteArrays.add(separator);
			if (entry.getValue() instanceof Path) {
				var path = (Path) entry.getValue();
				String mimeType = Files.probeContentType(path);
				byteArrays.add(("\"" + entry.getKey() + "\"; filename=\"" + path.getFileName()+ "\"\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
				byteArrays.add(Files.readAllBytes(path));
				byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
			}
			else {
				byteArrays.add(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
			}
		}
		byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
		
		return BodyPublishers.ofByteArrays(byteArrays);
	}
	
	
}
