package com.highform.affiliates.amazon;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class AmazonFetcher {
	  /*
	   * Your AWS Access Key ID, as taken from the AWS Your Account page.
	   */
	  private static final String AWS_ACCESS_KEY_ID = "AKIAIGYPKL36JNEPBM6A";

	  /*
	   * Your AWS Secret Key corresponding to the above ID, as taken from the AWS
	   * Your Account page.
	   */
	  private static final String AWS_SECRET_KEY = "wZEzudDEDKB571S4BP5L4il5X+O/jCQYRuZ9hMua";

	  /*
	   * Use one of the following end-points, according to the region you are
	   * interested in:
	   * 
	   * US: ecs.amazonaws.com
	   * CA: ecs.amazonaws.ca
	   * UK: ecs.amazonaws.co.uk
	   * DE: ecs.amazonaws.de
	   * FR: ecs.amazonaws.fr
	   * JP: ecs.amazonaws.jp
	   */
	  private static final String ENDPOINT = "ecs.amazonaws.com";
	  
	  private static volatile SignedRequestsHelper helper = null;

      public void getSignedRequestsHelper() throws InvalidKeyException, IllegalArgumentException, UnsupportedEncodingException, NoSuchAlgorithmException {
          if (helper == null) {
              synchronized(this) {
                  if (helper == null)
                	  helper = SignedRequestsHelper.getInstance(ENDPOINT, AWS_ACCESS_KEY_ID, AWS_SECRET_KEY);
              }
          }
      }
      
      public AmazonFetcher() throws InvalidKeyException, IllegalArgumentException, UnsupportedEncodingException, NoSuchAlgorithmException {
    	  if (helper == null) {
              synchronized(this) {
                  if (helper == null)
                	  helper = SignedRequestsHelper.getInstance(ENDPOINT, AWS_ACCESS_KEY_ID, AWS_SECRET_KEY);
              }
          }
      }
	  
	  public String fetchBook(String isbn, String associateTag) throws ParserConfigurationException, SAXException, IOException, InvalidKeyException, IllegalArgumentException, NoSuchAlgorithmException {
		    if (helper == null)
		    	getSignedRequestsHelper();
		    String requestUrl = null;
		    String title = null;
		    Map<String, String> params = new HashMap<String, String>();
		    params.put("Service", "AWSECommerceService");
		    params.put("Version", "2009-03-31");
		    params.put("Operation", "ItemLookup");
		    params.put("ItemId", isbn);
		    params.put("ResponseGroup", "OfferFull");
		    params.put("AssociateTag","associateTag");
		    params.put("MerchantId", "All");

		    requestUrl = helper.sign(params);
		    String content = null;
		    URLConnection connection = null;
		    try {
		      connection =  new URL(requestUrl).openConnection();
		      Scanner scanner = new Scanner(connection.getInputStream());
		      scanner.useDelimiter("\\Z");
		      content = scanner.next();
		    }catch ( Exception ex ) {
		        ex.printStackTrace();
		    }
		    return content;
	  }
	  
	  public static void main(String... args) throws InvalidKeyException, IllegalArgumentException, NoSuchAlgorithmException, ParserConfigurationException, SAXException, IOException {
		  AmazonFetcher bf = new AmazonFetcher();
		  String doc = bf.fetchBook("1479186724", "sjsu_2013_fall-20");
		  System.out.println(doc);
		  
	  }
}
